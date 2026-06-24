package com.reyzarium.eventforge.paymentservice.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record MockPaymentWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String providerInvoiceId,
        @NotBlank String status,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        @NotNull Instant occurredAt
) {
}
