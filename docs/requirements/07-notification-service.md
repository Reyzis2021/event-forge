# Notification Service — требования MVP

## 1. Назначение

`notification-service` отвечает за отправку уведомлений пользователям.

В MVP реальные email/SMS можно заменить на mock-отправку с записью в БД и лог.

## 2. Каналы

MVP поддерживает:

```text
EMAIL
IN_APP
```

Опционально:

```text
TELEGRAM
PUSH
```

## 3. Сущности

### notifications

```text
id UUID PK
user_id UUID NOT NULL
channel VARCHAR NOT NULL
type VARCHAR NOT NULL
status VARCHAR NOT NULL
subject VARCHAR NULL
body TEXT NOT NULL
payload JSONB NULL
created_at TIMESTAMP NOT NULL
sent_at TIMESTAMP NULL
failed_at TIMESTAMP NULL
error_message TEXT NULL
retry_count INT NOT NULL
```

status:

```text
PENDING
SENT
FAILED
CANCELLED
```

### processed_events

```text
event_id UUID PK
event_type VARCHAR NOT NULL
processed_at TIMESTAMP NOT NULL
```

Нужно для идемпотентности обработки Kafka events.

## 4. Kafka events consumed

Сервис должен слушать:

```text
user.registered
booking.created
booking.confirmed
booking.expired
payment.succeeded
payment.failed
ticket.issued
event.cancelled
```

## 5. Notification rules

### user.registered

Создать приветственное уведомление.

```text
channel: EMAIL
type: WELCOME
```

### booking.created

Сообщить пользователю, что бронь создана и нужно оплатить до `expiresAt`.

```text
channel: EMAIL + IN_APP
type: BOOKING_CREATED
```

### payment.succeeded

Сообщить, что оплата прошла.

```text
type: PAYMENT_SUCCEEDED
```

### payment.failed

Сообщить, что оплата не прошла.

```text
type: PAYMENT_FAILED
```

### ticket.issued

Сообщить, что билет выпущен.

```text
type: TICKET_ISSUED
```

### booking.expired

Сообщить, что бронь истекла.

```text
type: BOOKING_EXPIRED
```

### event.cancelled

Сообщить, что событие отменено.

```text
type: EVENT_CANCELLED
```

## 6. API

### 6.1. Получить мои in-app уведомления

```http
GET /api/v1/notifications/my?page=0&size=20
Authorization: Bearer <token>
```

Response:

```json
[
  {
    "notificationId": "uuid",
    "type": "TICKET_ISSUED",
    "channel": "IN_APP",
    "status": "SENT",
    "body": "Your ticket has been issued",
    "createdAt": "2026-06-17T10:05:00Z"
  }
]
```

### 6.2. Повторить failed уведомления

```http
POST /api/v1/admin/notifications/retry-failed
```

Role: `ADMIN`.

## 7. Отправка

В MVP сделать интерфейс:

```java
public interface NotificationSender {
    NotificationChannel channel();
    void send(NotificationMessage message);
}
```

Реализации:

- `MockEmailNotificationSender`
- `InAppNotificationSender`

## 8. Retry

Правила:

- если отправка упала, статус `FAILED`;
- retry job раз в минуту берет `FAILED`, где `retry_count < 3`;
- после успешной отправки статус `SENT`;
- если 3 попытки исчерпаны, оставить `FAILED`.

## 9. Идемпотентность

- Каждый Kafka event должен обрабатываться один раз по `eventId`.
- Повторный event не должен создавать дубль уведомления.

## 10. Ошибки

```text
NOTIFICATION_NOT_FOUND
NOTIFICATION_ACCESS_DENIED
NOTIFICATION_SEND_FAILED
EVENT_ALREADY_PROCESSED
```

## 11. Тесты

Обязательные тесты:

1. `user.registered` создает welcome notification.
2. `booking.created` создает booking notification.
3. Повторный event не создает дубль.
4. Failed notification ретраится.
5. После 3 неудачных retry остается `FAILED`.
6. Пользователь видит только свои уведомления.
7. Admin retry endpoint доступен только ADMIN.

## 12. Definition of Done

- Kafka events обрабатываются.
- Уведомления сохраняются.
- Mock email работает.
- In-app API работает.
- Retry работает.
- Идемпотентность есть.
