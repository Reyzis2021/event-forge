# Booking Service — требования MVP

## 1. Назначение

`booking-service` отвечает за бронирование билетов и защиту от overselling.

Это самый важный сервис проекта, потому что он показывает senior-level темы:

- конкурентный доступ;
- блокировки;
- идемпотентность;
- TTL брони;
- согласованность данных;
- Kafka events;
- outbox pattern.

## 2. Основной бизнес-сценарий

1. Пользователь выбирает событие и тип билета.
2. Пользователь отправляет запрос на создание брони.
3. Сервис проверяет, что событие опубликовано.
4. Сервис проверяет, что места еще есть.
5. Сервис создает бронь в статусе `PENDING_PAYMENT`.
6. Бронь живет 10 минут.
7. Если оплата пришла вовремя, бронь становится `CONFIRMED`.
8. Если оплата не пришла, бронь становится `EXPIRED`.

## 3. Сущности

### bookings

```text
id UUID PK
user_id UUID NOT NULL
event_id UUID NOT NULL
ticket_type_id UUID NOT NULL
quantity INT NOT NULL
unit_price NUMERIC(12,2) NOT NULL
total_price NUMERIC(12,2) NOT NULL
currency VARCHAR NOT NULL
status VARCHAR NOT NULL
expires_at TIMESTAMP NOT NULL
idempotency_key VARCHAR NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
UNIQUE(user_id, idempotency_key)
```

status:

```text
PENDING_PAYMENT
CONFIRMED
EXPIRED
CANCELLED
PAYMENT_FAILED
```

### ticket_type_inventory

```text
event_id UUID NOT NULL
ticket_type_id UUID NOT NULL
capacity INT NOT NULL
reserved INT NOT NULL
sold INT NOT NULL
version BIGINT NOT NULL
PRIMARY KEY(event_id, ticket_type_id)
```

Формула доступных мест:

```text
available = capacity - reserved - sold
```

## 4. API

### 4.1. Создать бронь

```http
POST /api/v1/bookings
Authorization: Bearer <token>
Idempotency-Key: <uuid>
```

Request:

```json
{
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2
}
```

Response `201`:

```json
{
  "bookingId": "uuid",
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-06-17T10:10:00Z",
  "amount": 100.00,
  "currency": "EUR"
}
```

Правила:

- `quantity` от 1 до 10;
- событие должно быть `PUBLISHED`;
- нельзя бронировать событие, которое уже началось;
- если мест недостаточно, вернуть 409;
- бронь живет 10 минут;
- повторный запрос с тем же `Idempotency-Key` должен вернуть ту же бронь;
- создание брони должно быть атомарным.

## 5. Защита от overselling

MVP должен выбрать один из вариантов:

### Вариант A — PostgreSQL pessimistic lock

При бронировании читать строку inventory:

```sql
SELECT * FROM ticket_type_inventory
WHERE event_id = ? AND ticket_type_id = ?
FOR UPDATE
```

Потом проверить:

```text
capacity - reserved - sold >= quantity
```

И увеличить `reserved`.

### Вариант B — Redis lock + DB transaction

Создать Redis lock на ключ:

```text
lock:inventory:{eventId}:{ticketTypeId}
```

После получения lock выполнить DB transaction.

Для MVP лучше использовать PostgreSQL `FOR UPDATE`, потому что проще доказать корректность.

## 6. Получить бронь

```http
GET /api/v1/bookings/{bookingId}
```

Правила:

- пользователь может получить только свою бронь;
- organizer/admin могут получить брони по своим событиям.

Response:

```json
{
  "bookingId": "uuid",
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "status": "PENDING_PAYMENT",
  "expiresAt": "2026-06-17T10:10:00Z",
  "amount": 100.00,
  "currency": "EUR"
}
```

## 7. Отменить бронь пользователем

```http
POST /api/v1/bookings/{bookingId}/cancel
```

Правила:

- отменить можно только `PENDING_PAYMENT`;
- при отмене уменьшить `reserved`;
- статус становится `CANCELLED`;
- отправить event `booking.cancelled`.

## 8. Expiration job

Сервис должен иметь scheduled job:

```text
каждую минуту найти PENDING_PAYMENT, где expires_at < now()
```

Для каждой такой брони:

- статус -> `EXPIRED`;
- уменьшить `reserved`;
- отправить event `booking.expired`.

Важно:

- job должна быть идемпотентной;
- при нескольких инстансах не должно быть двойного списания reserved;
- использовать `FOR UPDATE SKIP LOCKED`.

## 9. Internal API

### Подтвердить оплату

Вызывается payment-service или обрабатывается через Kafka event `payment.succeeded`.

```http
POST /internal/v1/bookings/{bookingId}/confirm-payment
```

Правила:

- если статус `PENDING_PAYMENT`, перевести в `CONFIRMED`;
- уменьшить `reserved`;
- увеличить `sold`;
- отправить `booking.confirmed`;
- если бронь уже `CONFIRMED`, вернуть success без изменений;
- если бронь `EXPIRED`, вернуть конфликт.

### Пометить failed payment

```http
POST /internal/v1/bookings/{bookingId}/fail-payment
```

Правила:

- `PENDING_PAYMENT` -> `PAYMENT_FAILED`;
- уменьшить `reserved`;
- отправить `booking.payment_failed`.

## 10. Kafka events consumed

### event.published

Создать inventory для каждого ticket type.

### event.cancelled

Отменить все активные `PENDING_PAYMENT` брони по событию.

### payment.succeeded

Подтвердить бронь.

### payment.failed

Пометить бронь как `PAYMENT_FAILED`.

## 11. Kafka events produced

### booking.created

```json
{
  "eventId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "amount": 100.00,
  "currency": "EUR",
  "expiresAt": "2026-06-17T10:10:00Z",
  "occurredAt": "2026-06-17T10:00:00Z"
}
```

### booking.confirmed

```json
{
  "bookingId": "uuid",
  "eventId": "uuid",
  "userId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "amount": 100.00,
  "currency": "EUR",
  "occurredAt": "2026-06-17T10:05:00Z"
}
```

### booking.expired

### booking.cancelled

### booking.payment_failed

## 12. Ошибки

```text
BOOKING_NOT_FOUND
BOOKING_ACCESS_DENIED
EVENT_NOT_AVAILABLE
TICKET_TYPE_NOT_AVAILABLE
INSUFFICIENT_TICKETS
BOOKING_ALREADY_CONFIRMED
BOOKING_ALREADY_EXPIRED
BOOKING_INVALID_STATUS
IDEMPOTENCY_KEY_REQUIRED
```

## 13. Тесты

Обязательные тесты:

1. Создание брони уменьшает available через `reserved`.
2. Повторный запрос с тем же idempotency key возвращает ту же бронь.
3. Нельзя забронировать больше capacity.
4. При 100 параллельных запросах на 1 место успешна только 1 бронь.
5. Expiration job переводит старую бронь в `EXPIRED`.
6. Expiration job уменьшает `reserved`.
7. Payment succeeded переводит бронь в `CONFIRMED`.
8. Повторный payment succeeded не увеличивает `sold` второй раз.
9. Payment failed освобождает reserved.
10. Event cancelled отменяет активные брони.

## 14. Definition of Done

- Бронь создается.
- Overselling невозможен.
- Idempotency работает.
- Expiration job работает.
- Payment events обрабатываются.
- Inventory корректно меняется.
- Есть concurrency integration test.
