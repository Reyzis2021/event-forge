# API Gateway — требования MVP

## 1. Назначение

`api-gateway` — единая точка входа во все backend-сервисы.

Он отвечает за:

- маршрутизацию запросов;
- проверку JWT;
- проброс пользовательского контекста;
- rate limiting;
- correlation id;
- агрегацию Swagger/OpenAPI ссылок;
- единый CORS.

## 2. Технологии

- Spring Cloud Gateway или Spring MVC Gateway
- Spring Security Resource Server
- Redis Rate Limiter
- OpenTelemetry
- Actuator

## 3. Основные маршруты

```text
/api/v1/auth/**          -> auth-service
/api/v1/events/**        -> event-service
/api/v1/bookings/**      -> booking-service
/api/v1/payments/**      -> payment-service
/api/v1/tickets/**       -> ticket-service
/api/v1/analytics/**     -> analytics-service
```

## 4. Security

### 4.1. Public endpoints

Без JWT доступны:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
GET  /api/v1/events
GET  /api/v1/events/{eventId}
GET  /actuator/health
```

### 4.2. Protected endpoints

Все остальные endpoint-ы требуют JWT.

### 4.3. Role-based access

Gateway должен проверять роли:

```text
/api/v1/events/organizer/**   -> ORGANIZER
/api/v1/analytics/**          -> ORGANIZER или ADMIN
/api/v1/admin/**              -> ADMIN
```

## 5. Headers

Gateway должен добавлять/пробрасывать:

```http
X-Request-Id
X-Correlation-Id
X-User-Id
X-User-Roles
Authorization
```

Если `X-Request-Id` не пришел, gateway генерирует новый UUID.

## 6. Rate limiting

Минимальные правила:

```text
POST /api/v1/auth/login       5 запросов / 1 минута / IP
POST /api/v1/bookings         20 запросов / 1 минута / userId
POST /api/v1/payments         20 запросов / 1 минута / userId
GET  /api/v1/events           100 запросов / 1 минута / IP
```

При превышении лимита вернуть:

```http
429 Too Many Requests
```

## 7. Error handling

Gateway должен приводить ошибки маршрутизации и security к единому формату:

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication required",
  "details": {},
  "traceId": "..."
}
```

## 8. Observability

Метрики:

- количество запросов по routeId;
- latency по routeId;
- количество 4xx/5xx;
- количество rate-limit reject-ов.

Логи:

- method;
- path;
- status;
- durationMs;
- userId;
- requestId;
- traceId.

## 9. Тесты

Обязательные тесты:

1. Public endpoint проходит без токена.
2. Protected endpoint без токена возвращает 401.
3. Endpoint с недостаточной ролью возвращает 403.
4. Gateway пробрасывает `X-User-Id`.
5. Rate limit возвращает 429.
6. Неизвестный route возвращает 404 в едином формате.

## 10. Definition of Done

- Все маршруты настроены.
- Security работает.
- Rate limit работает через Redis.
- Логи содержат requestId/traceId.
- Gateway участвует в distributed tracing.
- Есть интеграционные тесты.
