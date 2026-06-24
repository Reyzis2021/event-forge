package com.reyzarium.eventforge.paymentservice.application.service.impl;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.MockPaymentWebhookRequest;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.WebhookProcessingResponse;
import com.reyzarium.eventforge.paymentservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentWebhookService;
import com.reyzarium.eventforge.paymentservice.application.validator.PaymentWebhookValidator;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentWebhookEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentRepository;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentWebhookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private static final String MOCK_PROVIDER = "MOCK";

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository paymentWebhookRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMapper outboxMapper;
    private final PaymentWebhookValidator paymentWebhookValidator;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    @Transactional
    public WebhookProcessingResponse succeedMockInvoice(String providerInvoiceId) {
        return processMockInvoice(providerInvoiceId, PaymentWebhookValidator.SUCCEEDED_STATUS);
    }

    @Override
    @Transactional
    public WebhookProcessingResponse failMockInvoice(String providerInvoiceId) {
        return processMockInvoice(providerInvoiceId, PaymentWebhookValidator.FAILED_STATUS);
    }

    private WebhookProcessingResponse processMockInvoice(String providerInvoiceId, String status) {
        PaymentEntity payment = findPaymentByProviderInvoiceId(providerInvoiceId);
        Instant now = Instant.now(clock);
        MockPaymentWebhookRequest request = new MockPaymentWebhookRequest(
                "mock-evt-" + UUID.randomUUID(),
                providerInvoiceId,
                status,
                payment.getAmount(),
                payment.getCurrency(),
                now
        );

        return processMockProviderWebhook(request);
    }

    @Override
    @Transactional
    public WebhookProcessingResponse processMockProviderWebhook(MockPaymentWebhookRequest request) {
        return paymentWebhookRepository.findByProviderAndProviderEventId(MOCK_PROVIDER, request.providerEventId())
                .map(existingWebhook -> toResponse(findPaymentByProviderInvoiceId(existingWebhook.getProviderInvoiceId())))
                .orElseGet(() -> processNewWebhook(request));
    }

    private WebhookProcessingResponse processNewWebhook(MockPaymentWebhookRequest request) {
        Instant now = Instant.now(clock);
        PaymentEntity payment = findPaymentByProviderInvoiceId(request.providerInvoiceId());
        paymentWebhookValidator.validate(request, payment);

        paymentWebhookRepository.save(PaymentWebhookEntity.builder()
                .id(UUID.randomUUID())
                .provider(MOCK_PROVIDER)
                .providerEventId(request.providerEventId())
                .providerInvoiceId(request.providerInvoiceId())
                .payload(objectMapper.writeValueAsString(request))
                .processed(true)
                .createdAt(now)
                .processedAt(now)
                .build());

        applyWebhookStatus(request, payment, now);

        return toResponse(payment);
    }

    private void applyWebhookStatus(MockPaymentWebhookRequest request, PaymentEntity payment, Instant now) {
        if (paymentWebhookValidator.isSucceededStatus(request.status())) {
            if (payment.isSucceeded()) {
                return;
            }

            payment.markSucceeded(now);
            outboxEventRepository.save(outboxMapper.toPaymentSucceededOutbox(payment, now));
            return;
        }

        if (payment.isFailed()) {
            return;
        }

        payment.markFailed(now);
        outboxEventRepository.save(outboxMapper.toPaymentFailedOutbox(payment, now));
    }

    private PaymentEntity findPaymentByProviderInvoiceId(String providerInvoiceId) {
        return paymentRepository.findByProviderInvoiceId(providerInvoiceId)
                .orElseThrow(() -> new PaymentServiceException(
                        PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found"
                ));
    }

    private WebhookProcessingResponse toResponse(PaymentEntity payment) {
        return new WebhookProcessingResponse(payment.getId(), payment.getStatus());
    }
}
