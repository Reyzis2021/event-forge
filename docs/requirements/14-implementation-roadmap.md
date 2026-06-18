# Implementation Roadmap — порядок разработки MVP

## Phase 0 — Repository setup

Сделать:

- multi-module Gradle/Maven project;
- common code style;
- docker-compose skeleton;
- GitHub Actions skeleton;
- базовый README;
- общий error format;
- общий event envelope;
- общий logging format.

Результат:

- проект собирается;
- пустые сервисы стартуют;
- `/actuator/health` работает.

## Phase 1 — Auth + Gateway

Сделать:

- auth-service;
- register/login/refresh;
- JWT;
- roles;
- api-gateway routes;
- gateway security;
- rate limit на login.

Результат:

- можно получить JWT;
- protected endpoint закрыт;
- role-based access работает.

## Phase 2 — Event Service

Сделать:

- создание событий;
- ticket types;
- публикация;
- public search;
- cancel;
- event.published outbox.

Результат:

- organizer может создать и опубликовать событие;
- пользователь видит событие в public list.

## Phase 3 — Kafka + Outbox foundation

Сделать:

- Kafka topics;
- outbox table;
- outbox publisher;
- processed_events;
- базовый consumer template;
- DLQ.

Результат:

- event-service публикует `event.published`;
- booking-service может его обработать.

## Phase 4 — Booking Service

Сделать:

- inventory из `event.published`;
- create booking;
- PostgreSQL `FOR UPDATE` lock;
- idempotency key;
- expiration job;
- booking.created/expired events;
- concurrency test.

Результат:

- бронирование работает;
- overselling невозможен.

## Phase 5 — Payment Service

Сделать:

- create payment;
- mock invoice;
- webhook API;
- idempotent webhook;
- payment.succeeded/payment.failed;
- обработка booking.expired.

Результат:

- оплату можно симулировать;
- webhook подтверждает payment.

## Phase 6 — Booking confirmation

Сделать:

- booking-service consumer `payment.succeeded`;
- `PENDING_PAYMENT` -> `CONFIRMED`;
- reserved -> sold;
- booking.confirmed event.

Результат:

- после оплаты бронь подтверждается.

## Phase 7 — Ticket Service

Сделать:

- consumer `booking.confirmed`;
- ticket issuance;
- QR token;
- PDF/mock PDF;
- MinIO;
- get my tickets;
- validate ticket.

Результат:

- после оплаты пользователь получает билеты;
- билет можно проверить на входе.

## Phase 8 — Notifications

Сделать:

- consumers основных events;
- mock email;
- in-app notifications;
- retry failed;
- user notifications API.

Результат:

- пользователь получает уведомления по ключевым событиям.

## Phase 9 — Analytics

Сделать:

- read models;
- consumers events;
- event analytics API;
- platform analytics API;
- duplicate event protection.

Результат:

- organizer видит статистику события.

## Phase 10 — Observability

Сделать:

- structured logs;
- traceId/spanId;
- Prometheus metrics;
- Grafana dashboard;
- Jaeger/Tempo tracing;
- business metrics.

Результат:

- можно посмотреть latency, ошибки, Kafka processing, booking/payment metrics.

## Phase 11 — E2E and polish

Сделать:

- full happy path e2e;
- idempotency e2e;
- overselling e2e;
- README улучшить;
- добавить architecture diagram;
- добавить screenshots Grafana/Swagger;
- добавить Postman collection.

Результат:

- проект готов как portfolio MVP.
