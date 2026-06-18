# Payment Service — требования MVP

## 1. Назначение

`payment-service` отвечает за создание mock invoice, обработку webhook-ов и публикацию результата оплаты.

В MVP реального платежного провайдера нет. Нужно сделать mock-provider внутри сервиса или отдельный internal endpoint, который симулирует callback.

## 2. Главные senior-темы

- идемпотентность создания invoice;
- идемпотентность webhook;
- защита от двойного callback;
- state machine платежа;
- outbox pattern;
- retry и dead letter queue;
- audit trail.

## 3. Сущности

### payments

```text
id UUID PK
booking_id UUID NOT NULL
user_id UUID NOT NULL
amount NUMERIC(12,2) NOT NULL
currency VARCHAR NOT NULL
status VARCHAR NOT NULL
provider VARCHAR NOT NULL
provider_invoice_id VARCHAR UNIQUE NOT NULL
idempotency_key VARCHAR NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
paid_at TIMESTAMP NULL
failed_at TIMESTAMP NULL
version BIGINT NOT NULL
UNIQUE(user_id, idempotency_key)
UNIQUE(booking_id)
```

status:

```text
PENDING
SUCCEEDED
FAILED
CANCELLED
EXPIRED
```

### payment_webhooks

```text
id UUID PK
provider VARCHAR NOT NULL
provider_event_id VARCHAR UNIQUE NOT NULL
provider_invoice_id VARCHAR NOT NULL
payload JSONB NOT NULL
processed BOOLEAN NOT NULL
created_at TIMESTAMP NOT NULL
processed_at TIMESTAMP NULL
```

## 4. API

### 4.1. Создать платеж

```http
POST /api/v1/payments
Authorization: Bearer <token>
Idempotency-Key: <uuid>
```

Request:

```json
{
  "bookingId": "uuid"
}
```

Response `201`:

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "status": "PENDING",
  "amount": 100.00,
  "currency": "EUR",
  "providerInvoiceId": "mock-inv-123",
  "paymentUrl": "http://localhost:8080/mock-payments/mock-inv-123"
}
```

Правила:

- booking должен существовать и быть `PENDING_PAYMENT`;
- amount/currency брать из booking-service, а не из request;
- на одну booking может быть только один активный payment;
- повторный request с тем же `Idempotency-Key` возвращает тот же payment;
- повторный request для того же booking возвращает существующий payment.

## 5. Mock provider API

### 5.1. Симулировать успешную оплату

```http
POST /mock-provider/v1/invoices/{providerInvoiceId}/succeed
```

Должен создать webhook payload и вызвать внутреннюю обработку webhook.

### 5.2. Симулировать неуспешную оплату

```http
POST /mock-provider/v1/invoices/{providerInvoiceId}/fail
```

## 6. Webhook API

```http
POST /api/v1/payments/webhooks/mock-provider
```

Request:

```json
{
  "providerEventId": "evt-123",
  "providerInvoiceId": "mock-inv-123",
  "status": "SUCCEEDED",
  "amount": 100.00,
  "currency": "EUR",
  "occurredAt": "2026-06-17T10:05:00Z"
}
```

Правила:

- webhook должен быть идемпотентным по `providerEventId`;
- повторный webhook не должен повторно публиковать `payment.succeeded`;
- если payment уже `SUCCEEDED`, повторный succeeded вернуть 200;
- если payment уже `FAILED`, а пришел `SUCCEEDED`, в MVP вернуть конфликт и залогировать;
- amount/currency из webhook должны совпадать с payment;
- при несовпадении amount/currency вернуть ошибку и не менять статус.

## 7. Получить платеж

```http
GET /api/v1/payments/{paymentId}
```

Правила:

- пользователь может смотреть только свои платежи;
- admin может смотреть все.

Response:

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "status": "SUCCEEDED",
  "amount": 100.00,
  "currency": "EUR",
  "providerInvoiceId": "mock-inv-123",
  "createdAt": "2026-06-17T10:00:00Z",
  "paidAt": "2026-06-17T10:05:00Z"
}
```

## 8. Kafka events produced

### payment.created

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "amount": 100.00,
  "currency": "EUR",
  "providerInvoiceId": "mock-inv-123",
  "occurredAt": "2026-06-17T10:00:00Z"
}
```

### payment.succeeded

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "amount": 100.00,
  "currency": "EUR",
  "paidAt": "2026-06-17T10:05:00Z",
  "occurredAt": "2026-06-17T10:05:00Z"
}
```

### payment.failed

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "reason": "PROVIDER_DECLINED",
  "occurredAt": "2026-06-17T10:05:00Z"
}
```

## 9. Kafka events consumed

### booking.expired

Если payment еще `PENDING`, перевести в `EXPIRED`.

### booking.cancelled

Если payment еще `PENDING`, перевести в `CANCELLED`.

## 10. Внутренние вызовы

При создании payment сервис должен получить booking details:

```http
GET /internal/v1/bookings/{bookingId}/payment-info
```

Response:

```json
{
  "bookingId": "uuid",
  "userId": "uuid",
  "status": "PENDING_PAYMENT",
  "amount": 100.00,
  "currency": "EUR",
  "expiresAt": "2026-06-17T10:10:00Z"
}
```

## 11. Ошибки

```text
PAYMENT_NOT_FOUND
BOOKING_NOT_FOUND
BOOKING_NOT_PAYABLE
PAYMENT_ALREADY_EXISTS
PAYMENT_ALREADY_SUCCEEDED
PAYMENT_ALREADY_FAILED
WEBHOOK_ALREADY_PROCESSED
WEBHOOK_INVALID_AMOUNT
WEBHOOK_INVALID_SIGNATURE
IDEMPOTENCY_KEY_REQUIRED
```

## 12. Тесты

Обязательные тесты:

1. Создание payment создает invoice.
2. Повторный запрос с тем же idempotency key возвращает тот же payment.
3. Нельзя создать два payment на один booking.
4. Webhook succeeded переводит payment в `SUCCEEDED`.
5. Webhook succeeded публикует `payment.succeeded`.
6. Повторный webhook не публикует второй event.
7. Webhook с неверной суммой не меняет статус.
8. Payment failed публикует `payment.failed`.
9. booking.expired переводит pending payment в `EXPIRED`.
10. События пишутся через outbox.

## 13. Definition of Done

- Payment создается.
- Mock provider работает.
- Webhook идемпотентный.
- Повторные callback-и безопасны.
- Kafka events публикуются через outbox.
- Есть интеграционные тесты.
