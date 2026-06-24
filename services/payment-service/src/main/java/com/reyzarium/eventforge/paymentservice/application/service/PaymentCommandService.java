package com.reyzarium.eventforge.paymentservice.application.service;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentRequest;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentResponse;

import java.util.UUID;

public interface PaymentCommandService {

    CreatePaymentResponse createPayment(UUID userId, String idempotencyKey, CreatePaymentRequest request);
}
