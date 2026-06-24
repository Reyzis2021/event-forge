package com.reyzarium.eventforge.paymentservice.application.validator;

import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.dto.BookingPaymentInfoResponse;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class PaymentCommandValidator {

    private static final String PAYABLE_BOOKING_STATUS = "PENDING_PAYMENT";

    public String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PaymentServiceException(
                    PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED,
                    "Idempotency-Key header is required"
            );
        }
        return idempotencyKey.trim();
    }

    public void ensurePaymentDoesNotExist(PaymentEntity existingPayment) {
        throw new PaymentServiceException(
                PaymentErrorCode.PAYMENT_ALREADY_EXISTS,
                "Payment already exists for booking"
        );
    }

    public void validateBookingPayable(UUID userId, BookingPaymentInfoResponse booking, Instant now) {
        if (booking == null) {
            throw new PaymentServiceException(
                    PaymentErrorCode.BOOKING_NOT_FOUND,
                    "Booking not found"
            );
        }

        if (!userId.equals(booking.userId())) {
            throw new PaymentServiceException(
                    PaymentErrorCode.PAYMENT_ACCESS_DENIED,
                    "Booking belongs to another user"
            );
        }

        if (!PAYABLE_BOOKING_STATUS.equals(booking.status()) || !booking.expiresAt().isAfter(now)) {
            throw new PaymentServiceException(
                    PaymentErrorCode.BOOKING_NOT_PAYABLE,
                    "Booking is not payable"
            );
        }
    }
}
