package com.reyzarium.eventforge.paymentservice.application.service.impl;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.MockPaymentWebhookRequest;
import com.reyzarium.eventforge.paymentservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.paymentservice.application.validator.PaymentWebhookValidator;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentWebhookEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentRepository;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentWebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID PAYMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String PROVIDER_INVOICE_ID = "mock-inv-1";
    private static final String PROVIDER_EVENT_ID = "evt-1";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentWebhookRepository paymentWebhookRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxMapper outboxMapper;

    private PaymentWebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookServiceImpl(
                paymentRepository,
                paymentWebhookRepository,
                outboxEventRepository,
                outboxMapper,
                new PaymentWebhookValidator(),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void processMockProviderWebhook_whenPaymentIsPending_shouldMarkSucceededAndCreateOutbox() {
        PaymentEntity payment = pendingPayment();
        OutboxEventEntity outboxEvent = outboxEvent("payment.succeeded");
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));
        when(outboxMapper.toPaymentSucceededOutbox(payment, NOW)).thenReturn(outboxEvent);

        var response = service.processMockProviderWebhook(validWebhook());

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        verify(paymentWebhookRepository).save(any(PaymentWebhookEntity.class));
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void processMockProviderWebhook_whenPaymentIsPendingAndWebhookFailed_shouldMarkFailedAndCreateOutbox() {
        PaymentEntity payment = pendingPayment();
        OutboxEventEntity outboxEvent = outboxEvent("payment.failed");
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));
        when(outboxMapper.toPaymentFailedOutbox(payment, NOW)).thenReturn(outboxEvent);

        var response = service.processMockProviderWebhook(failedWebhook());

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailedAt()).isEqualTo(NOW);
        verify(paymentWebhookRepository).save(any(PaymentWebhookEntity.class));
        verify(outboxEventRepository).save(outboxEvent);
    }

    @Test
    void processMockProviderWebhook_whenProviderEventAlreadyProcessed_shouldReturnCurrentPaymentWithoutOutbox() {
        PaymentEntity payment = pendingPayment();
        payment.markSucceeded(NOW);
        PaymentWebhookEntity webhook = PaymentWebhookEntity.builder()
                .id(UUID.randomUUID())
                .provider("MOCK")
                .providerEventId(PROVIDER_EVENT_ID)
                .providerInvoiceId(PROVIDER_INVOICE_ID)
                .payload("{}")
                .processed(true)
                .createdAt(NOW)
                .processedAt(NOW)
                .build();
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.of(webhook));
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));

        var response = service.processMockProviderWebhook(validWebhook());

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verifyNoInteractions(outboxEventRepository, outboxMapper);
    }

    @Test
    void processMockProviderWebhook_whenPaymentAlreadySucceededWithNewEvent_shouldNotCreateSecondOutbox() {
        PaymentEntity payment = pendingPayment();
        payment.markSucceeded(NOW);
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));

        var response = service.processMockProviderWebhook(validWebhook());

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(paymentWebhookRepository).save(any(PaymentWebhookEntity.class));
        verifyNoInteractions(outboxEventRepository, outboxMapper);
    }

    @Test
    void processMockProviderWebhook_whenPaymentAlreadyFailedWithNewEvent_shouldNotCreateSecondOutbox() {
        PaymentEntity payment = pendingPayment();
        payment.markFailed(NOW);
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));

        var response = service.processMockProviderWebhook(failedWebhook());

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentWebhookRepository).save(any(PaymentWebhookEntity.class));
        verifyNoInteractions(outboxEventRepository, outboxMapper);
    }

    @Test
    void processMockProviderWebhook_whenSucceededPaymentGetsFailedWebhook_shouldThrowAlreadySucceeded() {
        PaymentEntity payment = pendingPayment();
        payment.markSucceeded(NOW);
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.processMockProviderWebhook(failedWebhook()))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_SUCCEEDED));
    }

    @Test
    void processMockProviderWebhook_whenAmountDoesNotMatch_shouldThrowInvalidAmount() {
        when(paymentWebhookRepository.findByProviderAndProviderEventId("MOCK", PROVIDER_EVENT_ID))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(pendingPayment()));
        MockPaymentWebhookRequest request = new MockPaymentWebhookRequest(
                PROVIDER_EVENT_ID,
                PROVIDER_INVOICE_ID,
                "SUCCEEDED",
                BigDecimal.valueOf(99),
                "EUR",
                NOW
        );

        assertThatThrownBy(() -> service.processMockProviderWebhook(request))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.WEBHOOK_INVALID_AMOUNT));
    }

    @Test
    void succeedMockInvoice_whenPaymentExists_shouldBuildSuccessfulWebhook() {
        PaymentEntity payment = pendingPayment();
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));
        when(paymentWebhookRepository.findByProviderAndProviderEventId(eq("MOCK"), any(String.class)))
                .thenReturn(Optional.empty());
        when(outboxMapper.toPaymentSucceededOutbox(payment, NOW)).thenReturn(outboxEvent("payment.succeeded"));

        var response = service.succeedMockInvoice(PROVIDER_INVOICE_ID);

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    void failMockInvoice_whenPaymentExists_shouldBuildFailedWebhook() {
        PaymentEntity payment = pendingPayment();
        when(paymentRepository.findByProviderInvoiceId(PROVIDER_INVOICE_ID)).thenReturn(Optional.of(payment));
        when(paymentWebhookRepository.findByProviderAndProviderEventId(eq("MOCK"), any(String.class)))
                .thenReturn(Optional.empty());
        when(outboxMapper.toPaymentFailedOutbox(payment, NOW)).thenReturn(outboxEvent("payment.failed"));

        var response = service.failMockInvoice(PROVIDER_INVOICE_ID);

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    private MockPaymentWebhookRequest validWebhook() {
        return new MockPaymentWebhookRequest(
                PROVIDER_EVENT_ID,
                PROVIDER_INVOICE_ID,
                "SUCCEEDED",
                BigDecimal.valueOf(100),
                "EUR",
                NOW
        );
    }

    private MockPaymentWebhookRequest failedWebhook() {
        return new MockPaymentWebhookRequest(
                PROVIDER_EVENT_ID,
                PROVIDER_INVOICE_ID,
                "FAILED",
                BigDecimal.valueOf(100),
                "EUR",
                NOW
        );
    }

    private PaymentEntity pendingPayment() {
        return PaymentEntity.builder()
                .id(PAYMENT_ID)
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .amount(BigDecimal.valueOf(100))
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .provider("MOCK")
                .providerInvoiceId(PROVIDER_INVOICE_ID)
                .idempotencyKey("idem-1")
                .createdAt(NOW.minusSeconds(60))
                .updatedAt(NOW.minusSeconds(60))
                .build();
    }

    private OutboxEventEntity outboxEvent(String eventType) {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(PAYMENT_ID)
                .aggregateType("PAYMENT")
                .eventType(eventType)
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
