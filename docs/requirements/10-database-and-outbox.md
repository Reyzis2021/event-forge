# Database & Outbox — требования MVP

## 1. Общие требования к БД

Каждый сервис имеет свою PostgreSQL database/schema.

Запрещено:

- читать таблицы другого сервиса напрямую;
- делать foreign key между БД разных сервисов;
- шарить entity-классы между сервисами.

Разрешено:

- общаться через REST internal API;
- общаться через Kafka events;
- дублировать данные в read model.

## 2. Миграции

Каждый сервис использует Flyway.

Структура:

```text
src/main/resources/db/migration/
  V1__init.sql
  V2__add_outbox.sql
```

Правила:

- миграции immutable;
- не редактировать уже примененную миграцию;
- nullable -> backfill -> not null при сложных изменениях.

## 3. Общая outbox table

Каждый event-producing сервис должен иметь таблицу:

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INT NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT NULL
);

CREATE INDEX idx_outbox_status_created_at
ON outbox_events(status, created_at);
```

status:

```text
PENDING
PUBLISHED
FAILED
```

## 4. Outbox publisher

Каждый сервис должен иметь scheduled/background publisher.

Алгоритм:

```text
1. Найти PENDING events через FOR UPDATE SKIP LOCKED.
2. Отправить в Kafka.
3. Если успешно — status = PUBLISHED, published_at = now.
4. Если ошибка — retry_count + 1.
5. Если retry_count >= 3 — status = FAILED.
```

SQL pattern:

```sql
SELECT * FROM outbox_events
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

## 5. Processed events table

Каждый event-consuming сервис должен иметь таблицу:

```sql
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL
);
```

Правило:

- перед обработкой event попытаться вставить `event_id`;
- если duplicate key — event уже обработан, ack и выйти;
- бизнес-изменение и вставка processed event должны быть в одной transaction.

## 6. Optimistic locking

Сущности, которые меняются конкурентно, должны иметь поле:

```text
version BIGINT NOT NULL
```

Обязательно для:

- events;
- ticket_types;
- bookings;
- inventory;
- payments;
- tickets.

## 7. Pessimistic locking

Для inventory в booking-service использовать:

```sql
SELECT * FROM ticket_type_inventory
WHERE event_id = ? AND ticket_type_id = ?
FOR UPDATE;
```

Это основной механизм защиты от overselling.

## 8. Индексы

Обязательные индексы:

Auth:

```sql
users(email)
refresh_tokens(token_hash)
```

Event:

```sql
events(status, starts_at)
events(organizer_id)
ticket_types(event_id)
```

Booking:

```sql
bookings(user_id)
bookings(event_id)
bookings(status, expires_at)
bookings(user_id, idempotency_key) unique
ticket_type_inventory(event_id, ticket_type_id) primary key
```

Payment:

```sql
payments(booking_id) unique
payments(provider_invoice_id) unique
payments(user_id, idempotency_key) unique
payment_webhooks(provider_event_id) unique
```

Ticket:

```sql
tickets(user_id)
tickets(event_id)
tickets(booking_id)
tickets(ticket_number) unique
tickets(qr_token_hash) unique
```

## 9. Тесты

Обязательные DB тесты:

1. Flyway миграции применяются на чистую БД.
2. Unique constraints реально работают.
3. Outbox event создается в одной transaction с бизнес-сущностью.
4. Rollback бизнес transaction откатывает outbox event.
5. Processed event защищает от дублей.
6. Inventory lock защищает от overselling.
