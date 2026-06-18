package com.reyzarium.eventforge.eventservice.api.organizer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTicketTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin(value = "0.00") BigDecimal price,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull @Min(1) Integer capacity
) {
}
