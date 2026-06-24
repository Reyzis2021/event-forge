package com.reyzarium.eventforge.paymentservice.api.payment.dto;

import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;

import java.util.UUID;

public record WebhookProcessingResponse(
        UUID paymentId,
        PaymentStatus status
) {
}
