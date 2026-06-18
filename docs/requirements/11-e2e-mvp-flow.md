# E2E MVP Flow — полный backend-сценарий

## 1. Цель

Описать полный end-to-end сценарий, который должен проходить после реализации MVP.

## 2. Предусловия

Система поднята через Docker Compose.

Доступны сервисы:

```text
api-gateway
auth-service
event-service
booking-service
payment-service
ticket-service
notification-service
analytics-service
postgres
redis
kafka
minio
prometheus
grafana
jaeger/tempo
```

## 3. Сценарий 1 — happy path покупки билета

### Шаг 1. Регистрация пользователя

```http
POST /api/v1/auth/register
```

Ожидаемый результат:

- пользователь создан;
- роль `USER` назначена;
- опубликован event `user.registered`;
- notification-service создал welcome notification.

### Шаг 2. Логин пользователя

```http
POST /api/v1/auth/login
```

Ожидаемый результат:

- получен JWT;
- получен refresh token.

### Шаг 3. Создание организатора

Для MVP можно:

- создать admin seed-ом;
- через admin endpoint выдать пользователю роль `ORGANIZER`.

```http
POST /api/v1/admin/users/{userId}/roles/organizer
```

Ожидаемый результат:

- пользователь получил роль `ORGANIZER`.

### Шаг 4. Организатор создает событие

```http
POST /api/v1/events/organizer/events
```

Ожидаемый результат:

- событие создано в статусе `DRAFT`;
- ticket type создан;
- event-service записал outbox event.

### Шаг 5. Организатор публикует событие

```http
POST /api/v1/events/organizer/events/{eventId}/publish
```

Ожидаемый результат:

- событие стало `PUBLISHED`;
- опубликован `event.published`;
- booking-service создал inventory.

### Шаг 6. Пользователь смотрит список событий

```http
GET /api/v1/events
```

Ожидаемый результат:

- опубликованное событие есть в списке;
- draft/cancelled events не видны.

### Шаг 7. Пользователь создает бронь

```http
POST /api/v1/bookings
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

Ожидаемый результат:

- создана booking в статусе `PENDING_PAYMENT`;
- `reserved` увеличился на 2;
- опубликован `booking.created`;
- notification-service создал уведомление о брони.

### Шаг 8. Пользователь создает payment

```http
POST /api/v1/payments
Idempotency-Key: <uuid>
```

Request:

```json
{
  "bookingId": "uuid"
}
```

Ожидаемый результат:

- создан payment в статусе `PENDING`;
- создан mock invoice;
- опубликован `payment.created`.

### Шаг 9. Mock-provider подтверждает оплату

```http
POST /mock-provider/v1/invoices/{providerInvoiceId}/succeed
```

Ожидаемый результат:

- payment стал `SUCCEEDED`;
- webhook записан как processed;
- опубликован `payment.succeeded`.

### Шаг 10. Booking подтверждается

После обработки `payment.succeeded` booking-service должен:

- перевести booking в `CONFIRMED`;
- уменьшить `reserved` на 2;
- увеличить `sold` на 2;
- опубликовать `booking.confirmed`.

### Шаг 11. Ticket-service выпускает билеты

После обработки `booking.confirmed` ticket-service должен:

- создать 2 tickets;
- сгенерировать QR для каждого;
- сгенерировать PDF/mock PDF;
- сохранить файл в MinIO;
- опубликовать 2 события `ticket.issued`.

### Шаг 12. Пользователь получает билеты

```http
GET /api/v1/tickets/my
```

Ожидаемый результат:

- пользователь видит 2 active tickets.

### Шаг 13. Проверка билета на входе

```http
POST /api/v1/tickets/validate
```

Request:

```json
{
  "qrToken": "raw-token-from-qr"
}
```

Ожидаемый результат:

- ticket меняет статус `ACTIVE` -> `USED`;
- опубликован `ticket.used`.

### Шаг 14. Организатор смотрит аналитику

```http
GET /api/v1/analytics/events/{eventId}
```

Ожидаемый результат:

```json
{
  "ticketsSold": 2,
  "revenue": 100.00,
  "bookingsCreated": 1,
  "bookingsConfirmed": 1,
  "paymentsSucceeded": 1
}
```

## 4. Сценарий 2 — идемпотентность брони

1. Отправить `POST /api/v1/bookings` с `Idempotency-Key=A`.
2. Повторить тот же запрос с `Idempotency-Key=A`.

Ожидаемый результат:

- второй запрос возвращает тот же `bookingId`;
- inventory не меняется второй раз;
- `booking.created` не публикуется второй раз.

## 5. Сценарий 3 — защита от overselling

Предусловие:

```text
event capacity = 1
```

Действие:

- отправить 100 параллельных запросов на создание брони.

Ожидаемый результат:

- успешен только 1 запрос;
- остальные получают 409 `INSUFFICIENT_TICKETS`;
- `reserved + sold <= capacity` всегда.

## 6. Сценарий 4 — повторный payment webhook

1. Создать booking.
2. Создать payment.
3. Отправить succeeded webhook.
4. Повторить тот же succeeded webhook с тем же `providerEventId`.

Ожидаемый результат:

- payment остается `SUCCEEDED`;
- `payment.succeeded` опубликован только один раз;
- booking подтвержден только один раз;
- tickets созданы только один раз.

## 7. Сценарий 5 — истечение брони

1. Создать booking.
2. Не оплачивать.
3. Дождаться `expiresAt`.
4. Запустить expiration job.

Ожидаемый результат:

- booking стал `EXPIRED`;
- reserved уменьшился;
- payment, если был pending, стал `EXPIRED`;
- notification-service создал уведомление;
- analytics увеличил expired counter.

## 8. Сценарий 6 — отмена события

1. Создать и опубликовать event.
2. Создать booking/payment/tickets.
3. Организатор отменяет event.

Ожидаемый результат:

- event стал `CANCELLED`;
- active bookings отменены;
- active tickets стали `CANCELLED`;
- notification-service создал уведомления;
- analytics обновил данные.

## 9. E2E тесты

Рекомендуемый стек:

- JUnit 5;
- Testcontainers;
- RestAssured;
- Awaitility;
- Kafka Testcontainers;
- PostgreSQL Testcontainers;
- Redis Testcontainers.

Минимальные e2e tests:

1. Full happy path.
2. Booking idempotency.
3. Payment webhook idempotency.
4. Overselling concurrency test.
5. Booking expiration.
