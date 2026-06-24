package com.reyzarium.eventforge.paymentservice.application.service;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.PaymentResponse;

import java.util.UUID;

public interface PaymentQueryService {

    PaymentResponse getPayment(UUID userId, UUID paymentId);
}
