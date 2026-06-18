package com.reyzarium.eventforge.bookingservice.application.service.impl;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingRequest;
import com.reyzarium.eventforge.bookingservice.application.mapper.BookingMapper;
import com.reyzarium.eventforge.bookingservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;
import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.EventServiceClient;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.dto.TicketTypeDetailsResponse;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryId;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.BookingRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.TicketTypeInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String IDEMPOTENCY_KEY = "00000000-0000-0000-0000-000000000004";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TicketTypeInventoryRepository inventoryRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private OutboxMapper outboxMapper;

    private BookingMapper bookingMapper;
    private BookingCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        bookingMapper = Mappers.getMapper(BookingMapper.class);
        service = new BookingCommandServiceImpl(
                bookingRepository,
                inventoryRepository,
                outboxEventRepository,
                eventServiceClient,
                bookingMapper,
                outboxMapper,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createBooking_whenRequestIsValid_shouldReserveInventoryAndCreateOutbox() {
        CreateBookingRequest request = validRequest(2);
        TicketTypeInventoryEntity inventory = inventory(10, 0, 0);
        OutboxEventEntity outboxEvent = outboxEvent();
        when(bookingRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(eventServiceClient.getTicketTypeDetails(EVENT_ID, TICKET_TYPE_ID)).thenReturn(publishedTicketType());
        when(inventoryRepository.findByIdForUpdate(EVENT_ID, TICKET_TYPE_ID)).thenReturn(Optional.of(inventory));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxMapper.toBookingCreatedOutbox(any(BookingEntity.class), eq(NOW))).thenReturn(outboxEvent);

        var response = service.createBooking(USER_ID, IDEMPOTENCY_KEY, request);

        ArgumentCaptor<BookingEntity> bookingCaptor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        BookingEntity savedBooking = bookingCaptor.getValue();
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(savedBooking.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(savedBooking.getItems()).hasSize(1);
        assertThat(inventory.getReserved()).isEqualTo(2);
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void createBooking_whenIdempotencyKeyAlreadyExists_shouldReturnExistingBooking() {
        BookingEntity existingBooking = existingBooking();
        when(bookingRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existingBooking));

        var response = service.createBooking(USER_ID, IDEMPOTENCY_KEY, validRequest(2));

        assertThat(response.bookingId()).isEqualTo(existingBooking.getId());
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        verifyNoInteractions(eventServiceClient, inventoryRepository, outboxEventRepository);
    }

    @Test
    void createBooking_whenIdempotencyKeyIsBlank_shouldThrowRequiredError() {
        assertThatThrownBy(() -> service.createBooking(USER_ID, " ", validRequest(1)))
                .isInstanceOfSatisfying(BookingServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BookingErrorCode.IDEMPOTENCY_KEY_REQUIRED));
    }

    @Test
    void createBooking_whenEventIsNotPublished_shouldThrowEventNotAvailable() {
        when(bookingRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(eventServiceClient.getTicketTypeDetails(EVENT_ID, TICKET_TYPE_ID))
                .thenReturn(new TicketTypeDetailsResponse(
                        EVENT_ID,
                        TICKET_TYPE_ID,
                        "DRAFT",
                        BigDecimal.valueOf(50),
                        "EUR",
                        10,
                        NOW.plusSeconds(3600)
                ));

        assertThatThrownBy(() -> service.createBooking(USER_ID, IDEMPOTENCY_KEY, validRequest(1)))
                .isInstanceOfSatisfying(BookingServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BookingErrorCode.EVENT_NOT_AVAILABLE));
    }

    @Test
    void createBooking_whenInventoryHasNotEnoughTickets_shouldThrowInsufficientTickets() {
        when(bookingRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(eventServiceClient.getTicketTypeDetails(EVENT_ID, TICKET_TYPE_ID)).thenReturn(publishedTicketType());
        when(inventoryRepository.findByIdForUpdate(EVENT_ID, TICKET_TYPE_ID))
                .thenReturn(Optional.of(inventory(1, 0, 0)));

        assertThatThrownBy(() -> service.createBooking(USER_ID, IDEMPOTENCY_KEY, validRequest(2)))
                .isInstanceOfSatisfying(BookingServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BookingErrorCode.INSUFFICIENT_TICKETS));
    }

    private CreateBookingRequest validRequest(int quantity) {
        return new CreateBookingRequest(EVENT_ID, TICKET_TYPE_ID, quantity);
    }

    private TicketTypeDetailsResponse publishedTicketType() {
        return new TicketTypeDetailsResponse(
                EVENT_ID,
                TICKET_TYPE_ID,
                "PUBLISHED",
                BigDecimal.valueOf(50),
                "EUR",
                100,
                NOW.plusSeconds(3600)
        );
    }

    private TicketTypeInventoryEntity inventory(int capacity, int reserved, int sold) {
        return TicketTypeInventoryEntity.builder()
                .id(new TicketTypeInventoryId(EVENT_ID, TICKET_TYPE_ID))
                .capacity(capacity)
                .reserved(reserved)
                .sold(sold)
                .build();
    }

    private BookingEntity existingBooking() {
        return bookingMapper.toPendingPaymentBooking(
                USER_ID,
                IDEMPOTENCY_KEY,
                validRequest(2),
                publishedTicketType(),
                NOW,
                NOW.plusSeconds(600)
        );
    }

    private OutboxEventEntity outboxEvent() {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("BOOKING")
                .eventType("booking.created")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
