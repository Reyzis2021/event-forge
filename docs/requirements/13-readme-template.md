# EventForge Backend

Production-like backend platform for event ticketing.

## Tech Stack

- Java 21+
- Spring Boot
- Spring Security
- PostgreSQL
- Flyway
- Kafka
- Redis
- MinIO
- OpenTelemetry
- Prometheus
- Grafana
- Testcontainers
- Docker Compose

## Services

```text
api-gateway
auth-service
event-service
booking-service
payment-service
ticket-service
notification-service
analytics-service
```

## Main Features

- User registration and JWT auth
- Organizer event management
- Event publishing
- Ticket booking with anti-overselling protection
- Payment mock provider
- Idempotent payment webhooks
- Ticket issuing with QR/PDF
- Notifications
- Analytics read model
- Kafka event-driven architecture
- Outbox pattern
- Observability
- Integration and E2E tests

## How to run

```bash
docker compose up --build
```

## MVP Flow

1. Register user.
2. Login.
3. Grant organizer role.
4. Create event.
5. Publish event.
6. Create booking.
7. Create payment.
8. Simulate successful payment.
9. Booking becomes confirmed.
10. Tickets are issued.
11. User gets tickets.
12. Organizer checks analytics.

## Architecture Decisions

### Why microservices?

The goal of this project is to demonstrate production-like backend architecture, not to create the simplest implementation.

### Why Kafka?

Kafka is used for asynchronous integration between services and eventual consistency.

### Why Outbox?

Outbox pattern prevents losing business events when database transaction succeeds but Kafka publishing fails.

### Why PostgreSQL lock for booking?

Booking inventory uses PostgreSQL `SELECT FOR UPDATE` to prevent overselling under concurrent requests.

## Important Guarantees

- The same booking request with the same idempotency key returns the same booking.
- The same payment webhook is processed only once.
- A ticket is issued only once for a confirmed booking.
- Sold + reserved tickets never exceed capacity.

## Documentation

See `docs/requirements`.
