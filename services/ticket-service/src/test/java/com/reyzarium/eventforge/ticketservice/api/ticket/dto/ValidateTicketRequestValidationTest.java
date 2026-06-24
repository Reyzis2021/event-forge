package com.reyzarium.eventforge.ticketservice.api.ticket.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidateTicketRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validate_whenQrTokenIsPresent_shouldHaveNoViolations() {
        var violations = validator.validate(new ValidateTicketRequest("raw-token"));

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_whenQrTokenIsBlank_shouldRejectRequest() {
        var violations = validator.validate(new ValidateTicketRequest(" "));

        assertThat(violations).isNotEmpty();
    }
}
