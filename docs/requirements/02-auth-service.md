# Auth Service — требования MVP

## 1. Назначение

`auth-service` отвечает за регистрацию, логин, refresh token, роли пользователей и выдачу JWT.

В MVP допустим собственный auth-service на Spring Security. Для более production-like варианта можно подключить Keycloak, но требования ниже описывают самостоятельный сервис.

## 2. Роли

```text
USER       обычный пользователь
ORGANIZER  организатор событий
ADMIN      администратор системы
```

## 3. Сущности

### users

```text
id UUID PK
email VARCHAR UNIQUE NOT NULL
password_hash VARCHAR NOT NULL
full_name VARCHAR NOT NULL
status VARCHAR NOT NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
```

status:

```text
ACTIVE
BLOCKED
DELETED
```

### user_roles

```text
user_id UUID FK users.id
role VARCHAR NOT NULL
```

### refresh_tokens

```text
id UUID PK
user_id UUID FK users.id
token_hash VARCHAR UNIQUE NOT NULL
expires_at TIMESTAMP NOT NULL
revoked BOOLEAN NOT NULL
created_at TIMESTAMP NOT NULL
```

## 4. API

### 4.1. Регистрация

```http
POST /api/v1/auth/register
```

Request:

```json
{
  "email": "user@example.com",
  "password": "StrongPass123!",
  "fullName": "Ivan Ivanov"
}
```

Response `201`:

```json
{
  "userId": "uuid",
  "email": "user@example.com"
}
```

Правила:

- email уникален;
- пароль минимум 8 символов;
- пароль хранить только как hash;
- роль по умолчанию `USER`.

### 4.2. Логин

```http
POST /api/v1/auth/login
```

Request:

```json
{
  "email": "user@example.com",
  "password": "StrongPass123!"
}
```

Response:

```json
{
  "accessToken": "jwt",
  "refreshToken": "token",
  "expiresIn": 900
}
```

Правила:

- access token живет 15 минут;
- refresh token живет 30 дней;
- refresh token хранится в БД как hash;
- при неверном пароле вернуть 401.

### 4.3. Refresh

```http
POST /api/v1/auth/refresh
```

Request:

```json
{
  "refreshToken": "token"
}
```

Response:

```json
{
  "accessToken": "jwt",
  "refreshToken": "new-token",
  "expiresIn": 900
}
```

Правила:

- старый refresh token отзывается;
- выдается новый refresh token;
- повторное использование старого refresh token должно вернуть 401.

### 4.4. Получить текущего пользователя

```http
GET /api/v1/auth/me
```

Response:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "fullName": "Ivan Ivanov",
  "roles": ["USER"]
}
```

### 4.5. Назначить роль организатора

```http
POST /api/v1/admin/users/{userId}/roles/organizer
```

Доступ: `ADMIN`.

## 5. JWT claims

JWT должен содержать:

```json
{
  "sub": "userId",
  "email": "user@example.com",
  "roles": ["USER"],
  "iat": 123,
  "exp": 456
}
```

## 6. Security требования

- Пароли не логировать.
- Refresh token не логировать.
- JWT secret или private key хранить через env/config.
- Для production-like варианта использовать RSA key pair.
- Login endpoint защищен rate limit-ом на gateway.

## 7. События Kafka

### user.registered

Публикуется после успешной регистрации.

```json
{
  "eventId": "uuid",
  "eventType": "user.registered",
  "occurredAt": "2026-06-17T10:00:00Z",
  "userId": "uuid",
  "email": "user@example.com"
}
```

## 8. Outbox

`user.registered` публиковать через outbox table.

### outbox_events

```text
id UUID PK
aggregate_type VARCHAR
aggregate_id UUID
event_type VARCHAR
payload JSONB
status VARCHAR
created_at TIMESTAMP
published_at TIMESTAMP NULL
```

## 9. Ошибки

```text
EMAIL_ALREADY_EXISTS
INVALID_CREDENTIALS
USER_BLOCKED
REFRESH_TOKEN_EXPIRED
REFRESH_TOKEN_REVOKED
ACCESS_DENIED
```

## 10. Тесты

Обязательные тесты:

1. Регистрация создает пользователя с ролью USER.
2. Нельзя зарегистрировать два одинаковых email.
3. Пароль хранится не в plain text.
4. Логин возвращает access и refresh token.
5. Неверный пароль возвращает 401.
6. Refresh отзывает старый refresh token.
7. Повторный refresh старым токеном возвращает 401.
8. JWT содержит userId и роли.
9. Событие `user.registered` попадает в outbox.

## 11. Definition of Done

- Пользователь может зарегистрироваться.
- Пользователь может залогиниться.
- JWT принимается gateway-ем.
- Refresh token rotation работает.
- Роли работают.
- Есть миграции Flyway.
- Есть unit и integration тесты.
