package com.reyzarium.eventforge.paymentservice.api.mockprovider;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.WebhookProcessingResponse;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mock-provider/v1/invoices")
public class MockProviderController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/{providerInvoiceId}/succeed")
    public WebhookProcessingResponse succeedInvoice(@PathVariable String providerInvoiceId) {
        return paymentWebhookService.succeedMockInvoice(providerInvoiceId);
    }

    @PostMapping("/{providerInvoiceId}/fail")
    public WebhookProcessingResponse failInvoice(@PathVariable String providerInvoiceId) {
        return paymentWebhookService.failMockInvoice(providerInvoiceId);
    }
}
