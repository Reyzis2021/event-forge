package com.reyzarium.eventforge.bookingservice.api.booking.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreateBookingRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_whenRequestIsValid_shouldHaveNoViolations() {
        var request = new CreateBookingRequest(UUID.randomUUID(), UUID.randomUUID(), 2);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validate_whenQuantityIsZero_shouldRejectRequest() {
        var request = new CreateBookingRequest(UUID.randomUUID(), UUID.randomUUID(), 0);

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity"));
    }

    @Test
    void validate_whenQuantityIsMoreThanTen_shouldRejectRequest() {
        var request = new CreateBookingRequest(UUID.randomUUID(), UUID.randomUUID(), 11);

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("quantity"));
    }
}
