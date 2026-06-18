# Analytics Service — требования MVP

## 1. Назначение

`analytics-service` отвечает за read-model статистику для организаторов и админов.

Он не должен ходить напрямую в БД других сервисов. Данные строятся на основе Kafka events.

## 2. Что показывает MVP

Для организатора:

- количество проданных билетов по событию;
- revenue по событию;
- количество активных броней;
- количество expired броней;
- конверсия booking -> payment;
- продажи по дням.

Для admin:

- общая выручка платформы;
- топ событий по продажам;
- количество пользователей;
- количество платежей;
- failed payment rate.

## 3. Сущности

### event_sales_summary

```text
event_id UUID PK
organizer_id UUID NOT NULL
tickets_sold INT NOT NULL
revenue NUMERIC(12,2) NOT NULL
currency VARCHAR NOT NULL
bookings_created INT NOT NULL
bookings_confirmed INT NOT NULL
bookings_expired INT NOT NULL
payments_succeeded INT NOT NULL
payments_failed INT NOT NULL
updated_at TIMESTAMP NOT NULL
```

### daily_sales_summary

```text
id UUID PK
event_id UUID NOT NULL
day DATE NOT NULL
tickets_sold INT NOT NULL
revenue NUMERIC(12,2) NOT NULL
currency VARCHAR NOT NULL
UNIQUE(event_id, day)
```

### platform_summary

```text
id UUID PK
day DATE NOT NULL UNIQUE
users_registered INT NOT NULL
events_published INT NOT NULL
bookings_created INT NOT NULL
tickets_sold INT NOT NULL
revenue NUMERIC(12,2) NOT NULL
payments_failed INT NOT NULL
updated_at TIMESTAMP NOT NULL
```

### processed_events

```text
event_id UUID PK
event_type VARCHAR NOT NULL
processed_at TIMESTAMP NOT NULL
```

## 4. Kafka events consumed

```text
user.registered
event.published
booking.created
booking.confirmed
booking.expired
payment.succeeded
payment.failed
ticket.issued
ticket.used
event.cancelled
```

## 5. Правила агрегации

### booking.created

- увеличить `bookings_created` в `event_sales_summary`;
- увеличить `bookings_created` в `platform_summary` за день.

### booking.confirmed

- увеличить `bookings_confirmed`;
- можно не увеличивать revenue здесь, чтобы не задвоить с `payment.succeeded`.

### booking.expired

- увеличить `bookings_expired`.

### payment.succeeded

- увеличить `payments_succeeded`;
- увеличить revenue;
- увеличить revenue в daily summary.

### payment.failed

- увеличить `payments_failed`.

### ticket.issued

- увеличить `tickets_sold`.

### user.registered

- увеличить `users_registered` в platform summary.

### event.published

- увеличить `events_published` в platform summary.

## 6. API

### 6.1. Получить аналитику события

```http
GET /api/v1/analytics/events/{eventId}
Authorization: Bearer <token>
```

Role: `ORGANIZER` или `ADMIN`.

Response:

```json
{
  "eventId": "uuid",
  "ticketsSold": 120,
  "revenue": 6000.00,
  "currency": "EUR",
  "bookingsCreated": 180,
  "bookingsConfirmed": 120,
  "bookingsExpired": 40,
  "paymentsSucceeded": 120,
  "paymentsFailed": 20,
  "conversionRate": 0.67
}
```

Правила:

- organizer может смотреть только свои события;
- admin может смотреть все.

### 6.2. Продажи по дням

```http
GET /api/v1/analytics/events/{eventId}/daily-sales?from=2026-06-01&to=2026-06-30
```

Response:

```json
[
  {
    "day": "2026-06-17",
    "ticketsSold": 20,
    "revenue": 1000.00,
    "currency": "EUR"
  }
]
```

### 6.3. Platform summary

```http
GET /api/v1/admin/analytics/platform?day=2026-06-17
```

Role: `ADMIN`.

Response:

```json
{
  "day": "2026-06-17",
  "usersRegistered": 20,
  "eventsPublished": 5,
  "bookingsCreated": 100,
  "ticketsSold": 70,
  "revenue": 3500.00,
  "paymentsFailed": 10
}
```

## 7. Идемпотентность

- Каждый Kafka event обрабатывается один раз по `eventId`.
- Повторное получение event не должно менять агрегаты второй раз.

## 8. Consistency

Analytics может быть eventually consistent.

В документации нужно явно написать:

```text
Analytics data is eventually consistent and may lag behind transactional services by several seconds.
```

## 9. Ошибки

```text
ANALYTICS_NOT_FOUND
ANALYTICS_ACCESS_DENIED
EVENT_ALREADY_PROCESSED
INVALID_DATE_RANGE
```

## 10. Тесты

Обязательные тесты:

1. `booking.created` увеличивает bookingsCreated.
2. Повторный `booking.created` не увеличивает счетчик второй раз.
3. `payment.succeeded` увеличивает revenue.
4. `ticket.issued` увеличивает ticketsSold.
5. Расчет conversionRate корректный.
6. Organizer не может смотреть чужую аналитику.
7. Admin может смотреть platform summary.
8. Daily sales строятся по дате события.

## 11. Definition of Done

- Read models строятся из Kafka events.
- Event processing идемпотентный.
- API аналитики работает.
- Доступы работают.
- Есть тесты.
