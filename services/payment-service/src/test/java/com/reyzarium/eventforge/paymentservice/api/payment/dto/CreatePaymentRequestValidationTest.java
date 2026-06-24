package com.reyzarium.eventforge.paymentservice.api.payment.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePaymentRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_whenRequestIsValid_shouldHaveNoViolations() {
        assertThat(validator.validate(new CreatePaymentRequest(UUID.randomUUID()))).isEmpty();
    }

    @Test
    void validate_whenBookingIdIsNull_shouldRejectRequest() {
        assertThat(validator.validate(new CreatePaymentRequest(null)))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("bookingId"));
    }
}
