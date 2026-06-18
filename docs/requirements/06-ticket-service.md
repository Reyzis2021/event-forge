# Ticket Service — требования MVP

## 1. Назначение

`ticket-service` отвечает за выпуск билетов после успешной оплаты и проверку билетов на входе.

## 2. Основной сценарий

1. Booking подтвержден.
2. Ticket-service получает `booking.confirmed`.
3. Сервис создает ticket для каждого купленного места.
4. Генерирует QR payload.
5. Генерирует PDF или mock PDF.
6. Сохраняет файл в MinIO.
7. Пользователь может получить список своих билетов.
8. Контролер может проверить билет на входе.
9. При первой успешной проверке билет становится `USED`.

## 3. Сущности

### tickets

```text
id UUID PK
booking_id UUID NOT NULL
user_id UUID NOT NULL
event_id UUID NOT NULL
ticket_type_id UUID NOT NULL
ticket_number VARCHAR UNIQUE NOT NULL
status VARCHAR NOT NULL
qr_token_hash VARCHAR UNIQUE NOT NULL
pdf_file_key VARCHAR NULL
issued_at TIMESTAMP NOT NULL
used_at TIMESTAMP NULL
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
version BIGINT NOT NULL
```

status:

```text
ACTIVE
USED
CANCELLED
REFUNDED
```

### ticket_issuance_log

```text
id UUID PK
booking_id UUID UNIQUE NOT NULL
status VARCHAR NOT NULL
created_at TIMESTAMP NOT NULL
error_message TEXT NULL
```

Нужен для идемпотентности выпуска билетов.

## 4. Kafka events consumed

### booking.confirmed

```json
{
  "bookingId": "uuid",
  "eventId": "uuid",
  "userId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2,
  "amount": 100.00,
  "currency": "EUR",
  "occurredAt": "2026-06-17T10:05:00Z"
}
```

Правила обработки:

- если tickets для booking уже созданы, повторный event не создает дубли;
- создать `quantity` билетов;
- каждый ticket должен иметь уникальный `ticketNumber`;
- каждый ticket должен иметь уникальный QR token;
- QR token хранить только как hash;
- исходный QR token возвращать только в момент создания или хранить безопасно в PDF payload.

### event.cancelled

Все active tickets по событию перевести в `CANCELLED`.

## 5. API

### 5.1. Получить мои билеты

```http
GET /api/v1/tickets/my
Authorization: Bearer <token>
```

Response:

```json
[
  {
    "ticketId": "uuid",
    "bookingId": "uuid",
    "eventId": "uuid",
    "ticketNumber": "EF-2026-000001",
    "status": "ACTIVE",
    "issuedAt": "2026-06-17T10:05:00Z"
  }
]
```

### 5.2. Получить билет

```http
GET /api/v1/tickets/{ticketId}
```

Правила:

- пользователь может получить только свой билет;
- organizer может получить билеты по своим событиям;
- admin может получить любой билет.

### 5.3. Скачать PDF билета

```http
GET /api/v1/tickets/{ticketId}/pdf
```

MVP-варианты:

- вернуть presigned URL на MinIO;
- или вернуть файл напрямую.

### 5.4. Проверить билет на входе

```http
POST /api/v1/tickets/validate
Authorization: Bearer <token>
```

Role: `ORGANIZER` или `ADMIN`.

Request:

```json
{
  "qrToken": "raw-token-from-qr"
}
```

Response success:

```json
{
  "ticketId": "uuid",
  "ticketNumber": "EF-2026-000001",
  "status": "USED",
  "valid": true,
  "message": "Ticket is valid"
}
```

Правила:

- если QR не найден, вернуть invalid;
- если билет `ACTIVE`, перевести в `USED`;
- если билет уже `USED`, вернуть valid=false и сообщение `Ticket already used`;
- повторная проверка не должна второй раз менять usedAt;
- использовать optimistic locking.

## 6. Kafka events produced

### ticket.issued

```json
{
  "ticketId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "ticketNumber": "EF-2026-000001",
  "occurredAt": "2026-06-17T10:05:00Z"
}
```

### ticket.used

```json
{
  "ticketId": "uuid",
  "bookingId": "uuid",
  "userId": "uuid",
  "eventId": "uuid",
  "ticketNumber": "EF-2026-000001",
  "usedAt": "2026-06-17T18:00:00Z",
  "occurredAt": "2026-06-17T18:00:00Z"
}
```

## 7. Файлы

PDF билета должен содержать:

- ticket number;
- event title;
- date/time;
- location;
- QR code;
- user id или masked user info.

В MVP можно сделать простой PDF без дизайна.

## 8. Ошибки

```text
TICKET_NOT_FOUND
TICKET_ACCESS_DENIED
TICKET_ALREADY_USED
TICKET_CANCELLED
QR_TOKEN_INVALID
TICKET_ISSUANCE_ALREADY_PROCESSED
PDF_GENERATION_FAILED
```

## 9. Тесты

Обязательные тесты:

1. `booking.confirmed` создает tickets по quantity.
2. Повторный `booking.confirmed` не создает дубли.
3. Ticket number уникален.
4. QR token hash уникален.
5. Пользователь видит свои билеты.
6. Пользователь не видит чужой билет.
7. Validate active ticket переводит его в `USED`.
8. Повторный validate возвращает `already used`.
9. event.cancelled отменяет active tickets.
10. ticket.issued публикуется через outbox.

## 10. Definition of Done

- Билеты выпускаются после оплаты.
- Дубли не создаются.
- PDF/QR генерируются.
- Проверка билета работает.
- Повторная проверка безопасна.
- Есть интеграционные тесты.
