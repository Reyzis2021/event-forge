package com.reyzarium.eventforge.bookingservice.api.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID eventId,
        @NotNull UUID ticketTypeId,
        @NotNull @Min(1) @Max(10) Integer quantity
) {
}
