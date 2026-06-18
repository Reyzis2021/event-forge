# Kafka Events — требования MVP

## 1. Общие правила

Все события должны иметь единый envelope.

```json
{
  "eventId": "uuid",
  "eventType": "booking.created",
  "eventVersion": 1,
  "occurredAt": "2026-06-17T10:00:00Z",
  "producer": "booking-service",
  "correlationId": "uuid",
  "payload": {}
}
```

## 2. Обязательные поля

```text
eventId        уникальный UUID события
eventType      тип события
eventVersion   версия контракта события
occurredAt     дата возникновения бизнес-события
producer       имя сервиса-источника
correlationId  id цепочки запроса
payload        данные события
```

## 3. Topics

MVP topics:

```text
user.events
event.events
booking.events
payment.events
ticket.events
notification.events
analytics.events optional
```

Для ошибок:

```text
*.dlq
```

## 4. Event list

### user.registered

Topic: `user.events`

Producer: `auth-service`

Consumers:

- notification-service
- analytics-service

Payload:

```json
{
  "userId": "uuid",
  "email": "user@example.com"
}
```

### event.published

Topic: `event.events`

Producer: `event-service`

Consumers:

- booking-service
- analytics-service

Payload:

```json
{
  "eventId": "uuid",
  "organizerId": "uuid",
  "ticketTypes": [
    {
      "ticketTypeId": "uuid",
      "capacity": 100,
      "price": 50.00,
      "currency": "EUR"
    }
  ]
}
```

### event.cancelled

Topic: `event.events`

Consumers:

- booking-service
- ticket-service
- notification-service
- analytics-service

Payload:

```json
{
  "eventId": "uuid",
  "organizerId": "uuid",
  "reason": "Venue unavailable"
}
```

### booking.created

Topic: `booking.events`

Consumers:

- payment-service optional
- notification-service
- analytics-service

Payload:

```json
{
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "amount": 100.00,
  "currency": "EUR",
  "expiresAt": "2026-06-17T10:10:00Z"
}
```

### booking.confirmed

Topic: `booking.events`

Consumers:

- ticket-service
- notification-service
- analytics-service

Payload:

```json
{
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "amount": 100.00,
  "currency": "EUR"
}
```

### booking.expired

Topic: `booking.events`

Consumers:

- payment-service
- notification-service
- analytics-service

Payload:

```json
{
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "reason": "PAYMENT_TIMEOUT"
}
```

### payment.created

Topic: `payment.events`

Consumers:

- analytics-service optional

Payload:

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "amount": 100.00,
  "currency": "EUR",
  "providerInvoiceId": "mock-inv-123"
}
```

### payment.succeeded

Topic: `payment.events`

Consumers:

- booking-service
- notification-service
- analytics-service

Payload:

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "amount": 100.00,
  "currency": "EUR",
  "paidAt": "2026-06-17T10:05:00Z"
}
```

### payment.failed

Topic: `payment.events`

Consumers:

- booking-service
- notification-service
- analytics-service

Payload:

```json
{
  "paymentId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "reason": "PROVIDER_DECLINED"
}
```

### ticket.issued

Topic: `ticket.events`

Consumers:

- notification-service
- analytics-service

Payload:

```json
{
  "ticketId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "ticketNumber": "EF-2026-000001"
}
```

### ticket.used

Topic: `ticket.events`

Consumers:

- analytics-service

Payload:

```json
{
  "ticketId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "ticketNumber": "EF-2026-000001",
  "usedAt": "2026-06-17T18:00:00Z"
}
```

## 5. Consumer requirements

Каждый consumer обязан:

- логировать eventId/eventType/correlationId;
- быть идемпотентным по eventId;
- не падать бесконечно на poison message;
- отправлять сообщение в DLQ после исчерпания retry;
- иметь метрики processed/failed/retried.

## 6. Retry

Рекомендуемая политика:

```text
max attempts: 3
backoff: 1s, 5s, 30s
then DLQ
```

## 7. Outbox pattern

Transactional сервисы не должны публиковать Kafka event напрямую внутри бизнес-логики.

Правильный flow:

1. Начать DB transaction.
2. Изменить бизнес-сущность.
3. Записать outbox event в ту же БД.
4. Commit.
5. Outbox publisher читает pending events.
6. Публикует в Kafka.
7. Помечает event как published.

## 8. Schema evolution

- `eventVersion` обязателен.
- Нельзя удалять поля без повышения major version.
- Новые поля должны быть backward-compatible.
- Consumers должны игнорировать неизвестные поля.
