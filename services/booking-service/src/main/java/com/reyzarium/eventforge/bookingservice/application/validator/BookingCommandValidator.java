package com.reyzarium.eventforge.bookingservice.application.validator;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingRequest;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.dto.TicketTypeDetailsResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class BookingCommandValidator {

    private static final String PUBLISHED_STATUS = "PUBLISHED";

    public String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BookingServiceException(
                    BookingErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                    "Idempotency-Key header is required"
            );
        }
        return idempotencyKey.trim();
    }

    public void validateTicketType(TicketTypeDetailsResponse ticketType,
                                   CreateBookingRequest request,
                                   Instant now) {
        if (ticketType == null || !request.eventId().equals(ticketType.eventId())
                || !request.ticketTypeId().equals(ticketType.ticketTypeId())) {
            throw new BookingServiceException(
                    BookingErrorCode.TICKET_TYPE_NOT_AVAILABLE,
                    "Ticket type is not available"
            );
        }

        if (!PUBLISHED_STATUS.equals(ticketType.status())) {
            throw new BookingServiceException(
                    BookingErrorCode.EVENT_NOT_AVAILABLE,
                    "Event is not available for booking"
            );
        }

        if (!ticketType.startsAt().isAfter(now)) {
            throw new BookingServiceException(
                    BookingErrorCode.EVENT_NOT_AVAILABLE,
                    "Event already started"
            );
        }
    }
}
