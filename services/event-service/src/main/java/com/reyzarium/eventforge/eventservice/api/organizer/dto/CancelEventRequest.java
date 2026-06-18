package com.reyzarium.eventforge.eventservice.api.organizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelEventRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
