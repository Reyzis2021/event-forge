package com.reyzarium.eventforge.eventservice.api.organizer.dto;

import com.reyzarium.eventforge.eventservice.api.validation.ValidEventSchedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

@ValidEventSchedule
public record CreateEventRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotBlank @Size(max = 100) String category,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 255) String location,
        @NotNull @Future Instant startsAt,
        @NotNull Instant endsAt,
        @NotEmpty @Size(max = 20) List<@Valid CreateTicketTypeRequest> ticketTypes
) {
}
