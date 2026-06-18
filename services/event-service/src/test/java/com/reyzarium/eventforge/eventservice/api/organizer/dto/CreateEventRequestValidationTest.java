package com.reyzarium.eventforge.eventservice.api.organizer.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateEventRequestValidationTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.byDefaultProvider()
                .configure()
                .clockProvider(() -> Clock.fixed(NOW, ZoneOffset.UTC))
                .buildValidatorFactory()
                .getValidator();
    }

    @Test
    void validate_whenRequestIsValid_shouldHaveNoViolations() {
        var violations = validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_whenStartsAtIsInPast_shouldRejectRequest() {
        var request = new CreateEventRequest(
                "Java Backend Meetup",
                "Spring Boot and Kafka",
                "IT",
                "Sofia",
                "Tech Park",
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600),
                List.of(validTicketType())
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("startsAt"));
    }

    @Test
    void validate_whenEndsAtIsBeforeStartsAt_shouldRejectRequest() {
        var request = new CreateEventRequest(
                "Java Backend Meetup",
                "Spring Boot and Kafka",
                "IT",
                "Sofia",
                "Tech Park",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(60),
                List.of(validTicketType())
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("endsAt"));
    }

    @Test
    void validate_whenTicketTypesAreEmpty_shouldRejectRequest() {
        var request = new CreateEventRequest(
                "Java Backend Meetup",
                "Spring Boot and Kafka",
                "IT",
                "Sofia",
                "Tech Park",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                List.of()
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("ticketTypes"));
    }

    @Test
    void validate_whenTicketPriceIsNegative_shouldRejectRequest() {
        var request = new CreateEventRequest(
                "Java Backend Meetup",
                "Spring Boot and Kafka",
                "IT",
                "Sofia",
                "Tech Park",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                List.of(new CreateTicketTypeRequest("Regular", BigDecimal.valueOf(-1), "EUR", 100))
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .isEqualTo("ticketTypes[0].price"));
    }

    private CreateEventRequest validRequest() {
        return new CreateEventRequest(
                "Java Backend Meetup",
                "Spring Boot and Kafka",
                "IT",
                "Sofia",
                "Tech Park",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                List.of(validTicketType())
        );
    }

    private CreateTicketTypeRequest validTicketType() {
        return new CreateTicketTypeRequest("Regular", BigDecimal.valueOf(50), "EUR", 100);
    }
}
