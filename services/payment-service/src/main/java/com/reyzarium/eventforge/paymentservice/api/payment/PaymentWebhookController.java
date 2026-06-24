package com.reyzarium.eventforge.paymentservice.api.payment;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.MockPaymentWebhookRequest;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.WebhookProcessingResponse;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/mock-provider")
    public WebhookProcessingResponse processMockProviderWebhook(
            @Valid @RequestBody MockPaymentWebhookRequest request
    ) {
        return paymentWebhookService.processMockProviderWebhook(request);
    }
}
