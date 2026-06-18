# Event Service — требования MVP

## 1. Назначение

`event-service` отвечает за создание, редактирование, публикацию и просмотр событий.

Сервис является source of truth для:

- информации о событии;
- организаторе;
- расписании;
- количестве мест;
- цене билета;
- статусе публикации.

## 2. Роли

- `USER` может смотреть опубликованные события.
- `ORGANIZER` может создавать и управлять своими событиями.
- `ADMIN` может смотреть и управлять всеми событиями.

## 3. Сущности

### events

```text
id UUID PK
organizer_id UUID NOT NULL
title VARCHAR NOT NULL
description TEXT
category VARCHAR NOT NULL
city VARCHAR NOT NULL
location VARCHAR NOT NULL
starts_at TIMESTAMP NOT NULL
ends_at TIMESTAMP NOT NULL
status VARCHAR NOT NULL
capacity INT NOT NULL
available_for_booking BOOLEAN NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
```

status:

```text
DRAFT
PUBLISHED
CANCELLED
FINISHED
```

### ticket_types

```text
id UUID PK
event_id UUID FK events.id
name VARCHAR NOT NULL
price NUMERIC(12,2) NOT NULL
currency VARCHAR NOT NULL
capacity INT NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
```

В MVP можно ограничиться одним ticket type на событие, но модель должна поддерживать несколько.

## 4. API

### 4.1. Создать событие

```http
POST /api/v1/events/organizer/events
Authorization: Bearer <token>
```

Role: `ORGANIZER`.

Request:

```json
{
  "title": "Java Backend Meetup",
  "description": "Meetup about Spring Boot and Kafka",
  "category": "IT",
  "city": "Sofia",
  "location": "Tech Park",
  "startsAt": "2026-08-01T18:00:00Z",
  "endsAt": "2026-08-01T21:00:00Z",
  "ticketTypes": [
    {
      "name": "Regular",
      "price": 50.00,
      "currency": "EUR",
      "capacity": 100
    }
  ]
}
```

Response `201`:

```json
{
  "eventId": "uuid",
  "status": "DRAFT"
}
```

Правила:

- событие создается в статусе `DRAFT`;
- `startsAt` должен быть в будущем;
- `endsAt` должен быть позже `startsAt`;
- capacity события равен сумме capacity ticket types;
- цена не может быть отрицательной.

### 4.2. Обновить событие

```http
PUT /api/v1/events/organizer/events/{eventId}
```

Role: `ORGANIZER`.

Правила:

- можно обновлять только свои события;
- нельзя редактировать `CANCELLED` и `FINISHED`;
- если событие уже `PUBLISHED`, нельзя уменьшать capacity ниже количества уже проданных/забронированных билетов;
- использовать optimistic locking через `version`.

### 4.3. Опубликовать событие

```http
POST /api/v1/events/organizer/events/{eventId}/publish
```

Role: `ORGANIZER`.

Правила:

- `DRAFT` -> `PUBLISHED`;
- должны быть заполнены title, location, startsAt, ticketTypes;
- после публикации отправить Kafka event `event.published`.

### 4.4. Отменить событие

```http
POST /api/v1/events/organizer/events/{eventId}/cancel
```

Role: `ORGANIZER`.

Request:

```json
{
  "reason": "Venue unavailable"
}
```

Правила:

- `PUBLISHED` -> `CANCELLED`;
- отправить Kafka event `event.cancelled`;
- booking-service должен отменить активные брони;
- payment-service/ticket-service в MVP могут только пометить, что требуется refund, без реального возврата.

### 4.5. Получить публичный список событий

```http
GET /api/v1/events?city=Sofia&category=IT&from=2026-08-01&to=2026-08-31&page=0&size=20
```

Правила:

- возвращать только `PUBLISHED`;
- поддержать пагинацию;
- поддержать фильтры по city/category/date.

### 4.6. Получить событие

```http
GET /api/v1/events/{eventId}
```

Response:

```json
{
  "eventId": "uuid",
  "title": "Java Backend Meetup",
  "description": "...",
  "category": "IT",
  "city": "Sofia",
  "location": "Tech Park",
  "startsAt": "2026-08-01T18:00:00Z",
  "endsAt": "2026-08-01T21:00:00Z",
  "status": "PUBLISHED",
  "ticketTypes": [
    {
      "ticketTypeId": "uuid",
      "name": "Regular",
      "price": 50.00,
      "currency": "EUR",
      "capacity": 100
    }
  ]
}
```

## 5. Внутреннее API для других сервисов

### Получить ticket type

```http
GET /internal/v1/events/{eventId}/ticket-types/{ticketTypeId}
```

Используется booking-service для валидации брони.

Response:

```json
{
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "status": "PUBLISHED",
  "price": 50.00,
  "currency": "EUR",
  "capacity": 100,
  "startsAt": "2026-08-01T18:00:00Z"
}
```

## 6. Kafka events

### event.created

```json
{
  "eventId": "uuid",
  "organizerId": "uuid",
  "status": "DRAFT",
  "occurredAt": "2026-06-17T10:00:00Z"
}
```

### event.published

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
  ],
  "occurredAt": "2026-06-17T10:00:00Z"
}
```

### event.cancelled

```json
{
  "eventId": "uuid",
  "organizerId": "uuid",
  "reason": "Venue unavailable",
  "occurredAt": "2026-06-17T10:00:00Z"
}
```

## 7. Outbox

Все Kafka events публиковать через outbox.

## 8. Ошибки

```text
EVENT_NOT_FOUND
EVENT_ACCESS_DENIED
EVENT_INVALID_STATUS
EVENT_ALREADY_PUBLISHED
EVENT_ALREADY_CANCELLED
TICKET_TYPE_NOT_FOUND
INVALID_EVENT_DATES
INVALID_TICKET_PRICE
INVALID_CAPACITY
OPTIMISTIC_LOCK_FAILED
```

## 9. Тесты

Обязательные тесты:

1. Организатор создает событие.
2. Событие создается в статусе `DRAFT`.
3. Нельзя создать событие с датой в прошлом.
4. Нельзя создать событие без ticket type.
5. Пользователь без роли ORGANIZER не может создать событие.
6. Организатор не может редактировать чужое событие.
7. Публикация меняет статус на `PUBLISHED`.
8. Публикация создает outbox event.
9. Public list возвращает только `PUBLISHED`.
10. Cancel меняет статус и создает `event.cancelled`.

## 10. Definition of Done

- CRUD для organizer-событий готов.
- Public search готов.
- Event publication готов.
- Event cancellation готов.
- Kafka events через outbox готовы.
- Internal API для booking-service готов.
- Есть миграции и тесты.
