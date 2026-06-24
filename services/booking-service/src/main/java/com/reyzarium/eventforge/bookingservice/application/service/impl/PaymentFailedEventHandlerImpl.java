package com.reyzarium.eventforge.bookingservice.application.service.impl;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentFailedEvent;
import com.reyzarium.eventforge.bookingservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.bookingservice.application.service.PaymentFailedEventHandler;
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
public class PaymentFailedEventHandlerImpl implements PaymentFailedEventHandler {

    private static final String PAYMENT_FAILED_EVENT_TYPE = "payment.failed";

    private final BookingRepository bookingRepository;
    private final TicketTypeInventoryRepository inventoryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMapper outboxMapper;
    private final PaymentEventValidator paymentEventValidator;
    private final Clock clock;

    @Override
    @Transactional
    public void handle(PaymentFailedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }

        BookingEntity booking = bookingRepository.findById(event.bookingId())
                .orElseThrow(() -> new BookingServiceException(
                        BookingErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found"
                ));
        paymentEventValidator.validateMatchesBooking(event, booking);

        if (booking.getStatus() == BookingStatus.PAYMENT_FAILED
                || booking.getStatus() == BookingStatus.EXPIRED
                || booking.getStatus() == BookingStatus.CANCELLED) {
            markProcessed(event);
            return;
        }

        paymentEventValidator.validateCanFail(booking);

        BookingItemEntity item = booking.getItems().getFirst();
        var inventory = inventoryRepository.findByIdForUpdate(booking.getEventId(), item.getTicketTypeId())
                .orElseThrow(() -> new BookingServiceException(
                        BookingErrorCode.TICKET_TYPE_NOT_AVAILABLE,
                        "Ticket type inventory not found"
                ));

        Instant now = Instant.now(clock);
        inventory.releaseReserved(item.getQuantity());
        booking.markPaymentFailed(now);
        outboxEventRepository.save(outboxMapper.toBookingPaymentFailedOutbox(booking, now));
        markProcessed(event);
    }

    private void markProcessed(PaymentFailedEvent event) {
        processedEventRepository.save(ProcessedEventEntity.builder()
                .eventId(event.eventId())
                .eventType(PAYMENT_FAILED_EVENT_TYPE)
                .processedAt(Instant.now(clock))
                .build());
    }
}
