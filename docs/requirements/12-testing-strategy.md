# Testing Strategy — требования MVP

## 1. Цель

Тесты должны доказывать, что система работает как production-like backend, а не просто компилируется.

## 2. Уровни тестирования

### 2.1. Unit tests

Проверяют бизнес-логику без Spring context.

Использовать:

- JUnit 5;
- Mockito;
- AssertJ.

Что тестировать unit-ами:

- domain services;
- validators;
- mappers частично;
- state transitions;
- price calculation;
- idempotency decision logic.

### 2.2. Integration tests

Проверяют работу с реальными инфраструктурными зависимостями.

Использовать:

- Spring Boot Test;
- Testcontainers;
- PostgreSQL;
- Kafka;
- Redis;
- WireMock.

Что тестировать:

- repository + Flyway;
- Kafka producer/consumer;
- Redis locks/rate limit;
- outbox publisher;
- REST clients;
- transaction boundaries.

### 2.3. Contract tests

Проверяют совместимость API между сервисами.

Минимум:

- booking-service client к event-service;
- payment-service client к booking-service;
- gateway routes к downstream services.

### 2.4. E2E tests

Проверяют полный бизнес-flow через gateway.

Использовать:

- RestAssured;
- Awaitility;
- Docker Compose или Testcontainers Compose.

## 3. Обязательные тесты по сервисам

### auth-service

- register success;
- duplicate email;
- login success;
- login invalid password;
- refresh token rotation;
- old refresh token rejected;
- JWT contains roles.

### event-service

- create draft event;
- publish event;
- cancel event;
- public search returns only published;
- organizer cannot update чужое событие;
- outbox event created.

### booking-service

- create booking;
- idempotent create booking;
- insufficient tickets;
- 100 parallel requests на 1 место;
- expiration job;
- payment succeeded confirms booking;
- repeated payment succeeded does not double-confirm.

### payment-service

- create payment;
- idempotent payment creation;
- duplicate booking payment blocked;
- webhook succeeded;
- duplicate webhook ignored;
- invalid amount rejected;
- outbox event created.

### ticket-service

- booking.confirmed creates tickets;
- duplicate booking.confirmed does not duplicate tickets;
- validate active ticket;
- validate used ticket;
- event.cancelled cancels tickets.

### notification-service

- event creates notification;
- duplicate event does not duplicate notification;
- retry failed notification;
- user sees only own notifications.

### analytics-service

- events update read models;
- duplicate event ignored;
- conversion rate calculation;
- access control.

## 4. Concurrency test для booking-service

Тест должен создать event capacity = 1 и запустить 100 параллельных запросов.

Проверки:

```text
success count = 1
conflict count = 99
inventory.reserved + inventory.sold = 1
bookings count = 1
```

## 5. Outbox tests

Для каждого event-producing сервиса:

1. Бизнес-операция создает outbox event.
2. При rollback outbox event не сохраняется.
3. Publisher публикует event в Kafka.
4. После публикации status = PUBLISHED.
5. При ошибке retry_count увеличивается.

## 6. Idempotency tests

Проверить:

- одинаковый `Idempotency-Key` возвращает тот же результат;
- разные `Idempotency-Key` создают разные операции;
- повторный webhook не меняет состояние;
- повторный Kafka event не меняет read model.

## 7. Performance smoke tests

Минимум для портфолио:

- k6 или Gatling script;
- 100 RPS на public events search;
- 20 RPS на booking creation;
- latency p95 в README.

## 8. Test naming convention

Формат:

```text
methodName_condition_expectedResult
```

Пример:

```java
createBooking_whenOnlyOneTicketAndManyParallelRequests_shouldCreateOnlyOneBooking()
```

## 9. Definition of Done

- Unit tests покрывают бизнес-логику.
- Integration tests используют реальные PostgreSQL/Kafka/Redis.
- Есть e2e happy path.
- Есть concurrency test.
- Есть idempotency tests.
- CI запускает все тесты.
