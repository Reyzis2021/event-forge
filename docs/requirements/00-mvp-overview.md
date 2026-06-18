# EventForge MVP — общие требования

## 1. Цель MVP

Сделать backend-only MVP платформы для продажи билетов на события.

MVP должен покрывать полный пользовательский сценарий:

1. Пользователь регистрируется/логинится.
2. Организатор создает событие.
3. Организатор публикует событие.
4. Пользователь смотрит список опубликованных событий.
5. Пользователь бронирует билет.
6. Система временно удерживает место.
7. Пользователь оплачивает бронь через mock-payment.
8. Система получает webhook от платежного провайдера.
9. Система подтверждает оплату.
10. Система выпускает билет.
11. Пользователь получает билет.
12. Контролер проверяет билет на входе.
13. Организатор смотрит аналитику продаж.

## 2. Архитектурная цель

Проект должен демонстрировать не CRUD, а production-like backend:

- микросервисная архитектура;
- event-driven взаимодействие через Kafka;
- PostgreSQL на каждый сервис;
- Redis для кэша, rate-limit и временных блокировок;
- идемпотентность команд и webhook-ов;
- защита от overselling;
- outbox pattern;
- distributed tracing;
- structured logging;
- metrics;
- интеграционные тесты через Testcontainers;
- документация API через OpenAPI.

## 3. Состав сервисов

MVP состоит из сервисов:

1. `api-gateway`
2. `auth-service`
3. `event-service`
4. `booking-service`
5. `payment-service`
6. `ticket-service`
7. `notification-service`
8. `analytics-service`

## 4. Общий стек

Backend:

- Java 21+
- Spring Boot 3.x/4.x
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Kafka
- Redis
- OpenAPI/Swagger
- MapStruct
- Lombok optional
- Testcontainers
- JUnit 5
- Mockito
- AssertJ
- WireMock

Infrastructure:

- Docker Compose
- Kubernetes manifests или Helm chart
- GitHub Actions
- Prometheus
- Grafana
- Loki
- OpenTelemetry
- Tempo или Jaeger
- MinIO для PDF/QR билетов

## 5. Нефункциональные требования

### 5.1. Безопасность

- Все пользовательские endpoint-ы, кроме публичного просмотра событий и auth, должны требовать JWT.
- Доступ к organizer endpoint-ам только для роли `ORGANIZER`.
- Доступ к admin endpoint-ам только для роли `ADMIN`.
- Внутренние сервисные endpoint-ы должны быть защищены service token-ом или mTLS-ready конфигурацией.

### 5.2. Идемпотентность

Идемпотентность обязательна для:

- создания брони;
- создания платежа;
- обработки payment webhook;
- выпуска билета;
- обработки Kafka events.

Для внешних команд использовать header:

```http
Idempotency-Key: <uuid>
```

### 5.3. Observability

Каждый сервис обязан отдавать:

- `/actuator/health`
- `/actuator/prometheus`
- traceId/spanId в логах;
- correlationId/requestId в логах;
- бизнес-метрики.

### 5.4. Ошибки

Все сервисы должны возвращать ошибки в едином формате:

```json
{
  "code": "BOOKING_NOT_FOUND",
  "message": "Booking not found",
  "details": {},
  "traceId": "..."
}
```

### 5.5. Версионирование API

Все внешние API должны быть под `/api/v1/...`.

## 6. Definition of Done для MVP

MVP считается готовым, если:

- можно поднять весь проект через `docker compose up`;
- Swagger доступен для каждого сервиса;
- полный happy-path проходит через API;
- overselling невозможен при конкурентных запросах;
- повторный webhook не создает второй билет;
- повторный payment request возвращает прежний invoice;
- Kafka events публикуются и читаются;
- есть минимум 1 интеграционный тест на каждый сервис;
- есть e2e-тест на полный сценарий покупки билета;
- есть dashboard в Grafana;
- в README описано, как запустить проект.
