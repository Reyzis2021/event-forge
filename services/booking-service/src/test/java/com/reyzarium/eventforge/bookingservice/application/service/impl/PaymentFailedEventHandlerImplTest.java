package com.reyzarium.eventforge.bookingservice.application.service.impl;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentFailedEvent;
import com.reyzarium.eventforge.bookingservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.bookingservice.application.validator.PaymentEventValidator;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;
import com.reyzarium.eventforge.bookingservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingItemEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.ProcessedEventEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.TicketTypeInventoryId;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.BookingRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.ProcessedEventRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.TicketTypeInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentFailedEventHandlerImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID EVENT_BUSINESS_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID TICKET_TYPE_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TicketTypeInventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMapper outboxMapper;

    private PaymentFailedEventHandlerImpl handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentFailedEventHandlerImpl(
                bookingRepository,
                inventoryRepository,
                processedEventRepository,
                outboxEventRepository,
                outboxMapper,
                new PaymentEventValidator(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void handle_whenBookingIsPendingPayment_shouldFailBookingReleaseInventoryAndCreateOutbox() {
        BookingEntity booking = pendingBooking();
        TicketTypeInventoryEntity inventory = inventory(10, 2, 0);
        OutboxEventEntity outboxEvent = outboxEvent();
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(inventoryRepository.findByIdForUpdate(EVENT_BUSINESS_ID, TICKET_TYPE_ID)).thenReturn(Optional.of(inventory));
        when(outboxMapper.toBookingPaymentFailedOutbox(booking, NOW)).thenReturn(outboxEvent);

        handler.handle(paymentFailedEvent());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(inventory.getReserved()).isZero();
        assertThat(inventory.getSold()).isZero();
        verify(outboxEventRepository).save(outboxEvent);

        ArgumentCaptor<ProcessedEventEntity> processedCaptor = ArgumentCaptor.forClass(ProcessedEventEntity.class);
        verify(processedEventRepository).save(processedCaptor.capture());
        assertThat(processedCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
        assertThat(processedCaptor.getValue().getEventType()).isEqualTo("payment.failed");
    }

    @Test
    void handle_whenEventAlreadyProcessed_shouldDoNothing() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(true);

        handler.handle(paymentFailedEvent());

        verifyNoInteractions(bookingRepository, inventoryRepository, outboxEventRepository, outboxMapper);
    }

    @Test
    void handle_whenBookingAlreadyExpired_shouldOnlyMarkEventProcessed() {
        BookingEntity booking = pendingBooking();
        booking.markExpired(NOW.minusSeconds(60));
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        handler.handle(paymentFailedEvent());

        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
        verifyNoInteractions(inventoryRepository, outboxEventRepository, outboxMapper);
    }

    @Test
    void handle_whenBookingIsConfirmed_shouldThrowInvalidStatus() {
        BookingEntity booking = pendingBooking();
        booking.markConfirmed(NOW.minusSeconds(60));
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> handler.handle(paymentFailedEvent()))
                .isInstanceOfSatisfying(BookingServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BookingErrorCode.BOOKING_INVALID_STATUS));
    }

    @Test
    void handle_whenPaymentEventDoesNotMatchBooking_shouldThrowMismatch() {
        when(processedEventRepository.existsById(EVENT_ID)).thenReturn(false);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(pendingBooking()));

        assertThatThrownBy(() -> handler.handle(new PaymentFailedEvent(
                EVENT_ID,
                PAYMENT_ID,
                BOOKING_ID,
                USER_ID,
                BigDecimal.valueOf(90),
                "EUR",
                NOW
        )))
                .isInstanceOfSatisfying(BookingServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(BookingErrorCode.PAYMENT_EVENT_MISMATCH));
    }

    private PaymentFailedEvent paymentFailedEvent() {
        return new PaymentFailedEvent(
                EVENT_ID,
                PAYMENT_ID,
                BOOKING_ID,
                USER_ID,
                BigDecimal.valueOf(100),
                "EUR",
                NOW
        );
    }

    private BookingEntity pendingBooking() {
        BookingEntity booking = BookingEntity.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .eventId(EVENT_BUSINESS_ID)
                .idempotencyKey("idem-1")
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(BigDecimal.valueOf(100))
                .currency("EUR")
                .expiresAt(NOW.plusSeconds(600))
                .createdAt(NOW.minusSeconds(60))
                .updatedAt(NOW.minusSeconds(60))
                .build();
        booking.addItem(BookingItemEntity.builder()
                .id(UUID.randomUUID())
                .ticketTypeId(TICKET_TYPE_ID)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(50))
                .currency("EUR")
                .createdAt(NOW.minusSeconds(60))
                .updatedAt(NOW.minusSeconds(60))
                .build());
        return booking;
    }

    private TicketTypeInventoryEntity inventory(int capacity, int reserved, int sold) {
        return TicketTypeInventoryEntity.builder()
                .id(new TicketTypeInventoryId(EVENT_BUSINESS_ID, TICKET_TYPE_ID))
                .capacity(capacity)
                .reserved(reserved)
                .sold(sold)
                .build();
    }

    private OutboxEventEntity outboxEvent() {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(BOOKING_ID)
                .aggregateType("BOOKING")
                .eventType("booking.payment_failed")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
