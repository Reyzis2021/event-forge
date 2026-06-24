package com.reyzarium.eventforge.ticketservice.api.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTicketRequest(
        @NotBlank String qrToken
) {
}
