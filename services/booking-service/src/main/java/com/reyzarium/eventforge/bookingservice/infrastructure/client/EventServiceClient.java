package com.reyzarium.eventforge.bookingservice.infrastructure.client;

import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.dto.TicketTypeDetailsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EventServiceClient {

    private final RestClient eventServiceRestClient;

    public TicketTypeDetailsResponse getTicketTypeDetails(UUID eventId, UUID ticketTypeId) {
        try {
            return eventServiceRestClient
                    .get()
                    .uri("/internal/v1/events/{eventId}/ticket-types/{ticketTypeId}", eventId, ticketTypeId)
                    .retrieve()
                    .body(TicketTypeDetailsResponse.class);
        } catch (RestClientException exception) {
            throw new BookingServiceException(
                    BookingErrorCode.EVENT_SERVICE_UNAVAILABLE,
                    "Event service is unavailable"
            );
        }
    }
}