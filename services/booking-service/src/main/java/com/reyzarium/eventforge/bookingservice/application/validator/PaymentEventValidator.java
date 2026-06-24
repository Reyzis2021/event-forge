package com.reyzarium.eventforge.bookingservice.application.validator;

import com.reyzarium.eventforge.bookingservice.application.event.PaymentFailedEvent;
import com.reyzarium.eventforge.bookingservice.application.event.PaymentSucceededEvent;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventValidator {

    public void validateMatchesBooking(PaymentSucceededEvent event, BookingEntity booking) {
        if (!event.userId().equals(booking.getUserId())
                || event.amount().compareTo(booking.getTotalAmount()) != 0
                || !event.currency().equals(booking.getCurrency())) {
            throwPaymentEventMismatch();
        }
    }

    public void validateMatchesBooking(PaymentFailedEvent event, BookingEntity booking) {
        if (!event.userId().equals(booking.getUserId())
                || event.amount().compareTo(booking.getTotalAmount()) != 0
                || !event.currency().equals(booking.getCurrency())) {
            throwPaymentEventMismatch();
        }
    }

    public void validateCanConfirm(BookingEntity booking) {
        if (!booking.isPendingPayment()) {
            throw new BookingServiceException(
                    BookingErrorCode.BOOKING_INVALID_STATUS,
                    "Only PENDING_PAYMENT booking can be confirmed"
            );
        }
    }

    public void validateCanFail(BookingEntity booking) {
        if (!booking.isPendingPayment()) {
            throw new BookingServiceException(
                    BookingErrorCode.BOOKING_INVALID_STATUS,
                    "Only PENDING_PAYMENT booking can be failed by payment event"
            );
        }
    }

    private void throwPaymentEventMismatch() {
        throw new BookingServiceException(
                BookingErrorCode.PAYMENT_EVENT_MISMATCH,
                "Payment event payload does not match booking"
        );
    }
}
