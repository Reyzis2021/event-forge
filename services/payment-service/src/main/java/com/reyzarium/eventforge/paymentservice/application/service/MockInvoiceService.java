package com.reyzarium.eventforge.paymentservice.application.service;

public interface MockInvoiceService {

    String createProviderInvoiceId();

    String createPaymentUrl(String providerInvoiceId);
}
