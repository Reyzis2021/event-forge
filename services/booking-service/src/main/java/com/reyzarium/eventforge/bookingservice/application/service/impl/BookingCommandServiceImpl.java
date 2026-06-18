package com.reyzarium.eventforge.bookingservice.application.service.impl;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingRequest;
import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingResponse;
import com.reyzarium.eventforge.bookingservice.application.mapper.BookingMapper;
import com.reyzarium.eventforge.bookingservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.bookingservice.application.service.BookingCommandService;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.EventServiceClient;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.dto.TicketTypeDetailsResponse;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryId;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.BookingRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.TicketTypeInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCommandServiceImpl implements BookingCommandService {

    private static final Duration BOOKING_TTL = Duration.ofMinutes(10);
    private static final String PUBLISHED_STATUS = "PUBLISHED";

    private final BookingRepository bookingRepository;
    private final TicketTypeInventoryRepository inventoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EventServiceClient eventServiceClient;
    private final BookingMapper bookingMapper;
    private final OutboxMapper outboxMapper;
    private final Clock clock;

    @Override
    @Transactional
    public CreateBookingResponse createBooking(UUID userId, String idempotencyKey, CreateBookingRequest request) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

        return bookingRepository.findByUserIdAndIdempotencyKey(userId, normalizedIdempotencyKey)
                .map(bookingMapper::toCreateResponse)
                .orElseGet(() -> createNewBooking(userId, normalizedIdempotencyKey, request));
    }

    private CreateBookingResponse createNewBooking(UUID userId,
                                                   String idempotencyKey,
                                                   CreateBookingRequest request) {
        Instant now = Instant.now(clock);
        TicketTypeDetailsResponse ticketType = eventServiceClient.getTicketTypeDetails(
                request.eventId(),
                request.ticketTypeId()
        );
        validateTicketType(ticketType, request, now);

        TicketTypeInventoryEntity inventory = getOrCreateInventory(ticketType);
        inventory.reserve(request.quantity());

        var booking = bookingMapper.toPendingPaymentBooking(
                userId,
                idempotencyKey,
                request,
                ticketType,
                now,
                now.plus(BOOKING_TTL)
        );

        var savedBooking = bookingRepository.save(booking);
        outboxEventRepository.save(outboxMapper.toBookingCreatedOutbox(savedBooking, now));

        return bookingMapper.toCreateResponse(savedBooking);
    }

    private TicketTypeInventoryEntity getOrCreateInventory(TicketTypeDetailsResponse ticketType) {
        return inventoryRepository.findByIdForUpdate(ticketType.eventId(), ticketType.ticketTypeId())
                .orElseGet(() -> inventoryRepository.save(TicketTypeInventoryEntity.builder()
                        .id(new TicketTypeInventoryId(ticketType.eventId(), ticketType.ticketTypeId()))
                        .capacity(ticketType.capacity())
                        .reserved(0)
                        .sold(0)
                        .build()));
    }

    private void validateTicketType(TicketTypeDetailsResponse ticketType,
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

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BookingServiceException(
                    BookingErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                    "Idempotency-Key header is required"
            );
        }
        return idempotencyKey.trim();
    }
}
