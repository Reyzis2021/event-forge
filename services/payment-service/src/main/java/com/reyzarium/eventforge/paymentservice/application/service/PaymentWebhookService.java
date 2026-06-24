package com.reyzarium.eventforge.paymentservice.application.service;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.MockPaymentWebhookRequest;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.WebhookProcessingResponse;

public interface PaymentWebhookService {

    WebhookProcessingResponse succeedMockInvoice(String providerInvoiceId);

    WebhookProcessingResponse failMockInvoice(String providerInvoiceId);

    WebhookProcessingResponse processMockProviderWebhook(MockPaymentWebhookRequest request);
}
