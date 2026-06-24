package com.reyzarium.eventforge.paymentservice.api.payment.dto;

import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentResponse(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String providerInvoiceId,
        String paymentUrl
) {
}
