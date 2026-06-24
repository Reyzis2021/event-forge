package com.reyzarium.eventforge.paymentservice.application.service.impl;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentRequest;
import com.reyzarium.eventforge.paymentservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.paymentservice.application.mapper.PaymentMapper;
import com.reyzarium.eventforge.paymentservice.application.service.MockInvoiceService;
import com.reyzarium.eventforge.paymentservice.application.validator.PaymentCommandValidator;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;
import com.reyzarium.eventforge.paymentservice.domain.outbox.OutboxStatus;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.BookingServiceClient;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.dto.BookingPaymentInfoResponse;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.OutboxEventEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-06-18T10:00:00Z");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final String IDEMPOTENCY_KEY = "00000000-0000-0000-0000-000000000004";
    private static final String PROVIDER_INVOICE_ID = "mock-inv-1";
    private static final String PAYMENT_URL = "http://localhost:8083/mock-provider/v1/invoices/mock-inv-1";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @Mock
    private MockInvoiceService mockInvoiceService;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private PaymentCommandServiceImpl service;
    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        paymentMapper = Mappers.getMapper(PaymentMapper.class);
        service = new PaymentCommandServiceImpl(
                paymentRepository,
                bookingServiceClient,
                mockInvoiceService,
                paymentMapper,
                outboxMapper,
                outboxEventRepository,
                new PaymentCommandValidator(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createPayment_whenBookingIsPayable_shouldCreatePendingPayment() {
        when(paymentRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());
        when(bookingServiceClient.getPaymentInfo(BOOKING_ID)).thenReturn(payableBooking(USER_ID));
        when(mockInvoiceService.createProviderInvoiceId()).thenReturn(PROVIDER_INVOICE_ID);
        when(mockInvoiceService.createPaymentUrl(PROVIDER_INVOICE_ID)).thenReturn(PAYMENT_URL);
        when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxMapper.toPaymentCreatedOutbox(any(PaymentEntity.class), any(Instant.class))).thenReturn(outboxEvent());

        var response = service.createPayment(USER_ID, IDEMPOTENCY_KEY, new CreatePaymentRequest(BOOKING_ID));

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(captor.capture());
        PaymentEntity savedPayment = captor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo("100.00");
        assertThat(savedPayment.getCurrency()).isEqualTo("EUR");
        assertThat(savedPayment.getProvider()).isEqualTo("MOCK");
        assertThat(savedPayment.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(response.providerInvoiceId()).isEqualTo(PROVIDER_INVOICE_ID);
        assertThat(response.paymentUrl()).isEqualTo(PAYMENT_URL);
        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    void createPayment_whenIdempotencyKeyAlreadyExists_shouldReturnExistingPayment() {
        PaymentEntity existingPayment = existingPayment();
        when(paymentRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existingPayment));
        when(mockInvoiceService.createPaymentUrl(PROVIDER_INVOICE_ID)).thenReturn(PAYMENT_URL);

        var response = service.createPayment(USER_ID, IDEMPOTENCY_KEY, new CreatePaymentRequest(BOOKING_ID));

        assertThat(response.paymentId()).isEqualTo(existingPayment.getId());
        assertThat(response.providerInvoiceId()).isEqualTo(PROVIDER_INVOICE_ID);
        verifyNoInteractions(bookingServiceClient);
    }

    @Test
    void createPayment_whenPaymentAlreadyExistsForBooking_shouldThrowAlreadyExists() {
        when(paymentRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(existingPayment()));

        assertThatThrownBy(() -> service.createPayment(USER_ID, IDEMPOTENCY_KEY, new CreatePaymentRequest(BOOKING_ID)))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_EXISTS));
    }

    @Test
    void createPayment_whenBookingBelongsToAnotherUser_shouldThrowAccessDenied() {
        when(paymentRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());
        when(bookingServiceClient.getPaymentInfo(BOOKING_ID)).thenReturn(payableBooking(OTHER_USER_ID));

        assertThatThrownBy(() -> service.createPayment(USER_ID, IDEMPOTENCY_KEY, new CreatePaymentRequest(BOOKING_ID)))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.PAYMENT_ACCESS_DENIED));
    }

    @Test
    void createPayment_whenBookingIsExpired_shouldThrowNotPayable() {
        when(paymentRepository.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(paymentRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());
        when(bookingServiceClient.getPaymentInfo(BOOKING_ID)).thenReturn(new BookingPaymentInfoResponse(
                BOOKING_ID,
                USER_ID,
                "PENDING_PAYMENT",
                BigDecimal.valueOf(100),
                "EUR",
                NOW.minusSeconds(1)
        ));

        assertThatThrownBy(() -> service.createPayment(USER_ID, IDEMPOTENCY_KEY, new CreatePaymentRequest(BOOKING_ID)))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.BOOKING_NOT_PAYABLE));
    }

    @Test
    void createPayment_whenIdempotencyKeyIsBlank_shouldThrowRequiredError() {
        assertThatThrownBy(() -> service.createPayment(USER_ID, " ", new CreatePaymentRequest(BOOKING_ID)))
                .isInstanceOfSatisfying(PaymentServiceException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.IDEMPOTENCY_KEY_REQUIRED));
    }

    private BookingPaymentInfoResponse payableBooking(UUID userId) {
        return new BookingPaymentInfoResponse(
                BOOKING_ID,
                userId,
                "PENDING_PAYMENT",
                BigDecimal.valueOf(100),
                "EUR",
                NOW.plusSeconds(600)
        );
    }

    private PaymentEntity existingPayment() {
        return paymentMapper.toPendingPayment(
                USER_ID,
                IDEMPOTENCY_KEY,
                payableBooking(USER_ID),
                PROVIDER_INVOICE_ID,
                NOW
        );
    }

    private OutboxEventEntity outboxEvent() {
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .aggregateType("PAYMENT")
                .eventType("payment.created")
                .eventVersion(1)
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .createdAt(NOW)
                .retryCount(0)
                .build();
    }
}
