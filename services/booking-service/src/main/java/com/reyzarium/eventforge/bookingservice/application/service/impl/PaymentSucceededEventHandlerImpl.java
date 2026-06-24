package com.reyzarium.eventforge.bookingservice.application.service.impl;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentSucceededEvent;
import com.reyzarium.eventforge.bookingservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.bookingservice.application.service.PaymentSucceededEventHandler;
import com.reyzarium.eventforge.bookingservice.application.validator.PaymentEventValidator;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingItemEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.ProcessedEventEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.BookingRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.ProcessedEventRepository;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.TicketTypeInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentSucceededEventHandlerImpl implements PaymentSucceededEventHandler {

    private static final String PAYMENT_SUCCEEDED_EVENT_TYPE = "payment.succeeded";

    private final BookingRepository bookingRepository;
    private final TicketTypeInventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMapper outboxMapper;
    private final PaymentEventValidator paymentEventValidator;
    private final Clock clock;

    @Override
    @Transactional
    public void handle(PaymentSucceededEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        BookingEntity booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new BookingServiceException(
                        BookingErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found"
                ));
        paymentEventValidator.validateMatchesBooking(event, booking);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            markProcessed(event);
            return;
        }

        paymentEventValidator.validateCanConfirm(booking);

        BookingItemEntity item = booking.getItems().getFirst();
        var inventory = inventoryRepository.findByIdForUpdate(booking.getEventId(), item.getTicketTypeId())
                .orElseThrow(() -> new BookingServiceException(
                        BookingErrorCode.TICKET_TYPE_NOT_AVAILABLE,
                        "Ticket type inventory not found"
                ));

        Instant now = Instant.now(clock);
        inventory.confirmReserved(item.getQuantity());
        booking.markConfirmed(now);
        outboxEventRepository.save(outboxMapper.toBookingConfirmedOutbox(booking, now));
        markProcessed(event);
    }

    private void markProcessed(PaymentSucceededEvent event) {
        processedEventRepository.save(ProcessedEventEntity.builder()
                .eventId(event.eventId())
                .eventType(PAYMENT_SUCCEEDED_EVENT_TYPE)
                .processedAt(Instant.now(clock))
                .build());
    }
}
