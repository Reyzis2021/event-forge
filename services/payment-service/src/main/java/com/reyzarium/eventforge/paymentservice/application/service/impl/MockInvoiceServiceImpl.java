package com.reyzarium.eventforge.paymentservice.application.service.impl;

import com.reyzarium.eventforge.paymentservice.application.service.MockInvoiceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MockInvoiceServiceImpl implements MockInvoiceService {

    private final String publicBaseUrl;

    public MockInvoiceServiceImpl(@Value("${payment-service.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public String createProviderInvoiceId() {
        return "mock-inv-" + UUID.randomUUID();
    }

    @Override
    public String createPaymentUrl(String providerInvoiceId) {
        return publicBaseUrl + "/mock-provider/v1/invoices/" + providerInvoiceId;
    }
}
