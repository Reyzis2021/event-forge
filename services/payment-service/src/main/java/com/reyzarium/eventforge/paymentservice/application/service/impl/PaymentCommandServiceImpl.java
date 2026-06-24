package com.reyzarium.eventforge.paymentservice.application.service.impl;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentRequest;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentResponse;
import com.reyzarium.eventforge.paymentservice.application.mapper.OutboxMapper;
import com.reyzarium.eventforge.paymentservice.application.mapper.PaymentMapper;
import com.reyzarium.eventforge.paymentservice.application.service.MockInvoiceService;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentCommandService;
import com.reyzarium.eventforge.paymentservice.application.validator.PaymentCommandValidator;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.BookingServiceClient;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.dto.BookingPaymentInfoResponse;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.OutboxEventRepository;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentCommandServiceImpl implements PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;
    private final MockInvoiceService mockInvoiceService;
    private final PaymentMapper paymentMapper;
    private final OutboxMapper outboxMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentCommandValidator paymentCommandValidator;
    private final Clock clock;

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(UUID userId, String idempotencyKey, CreatePaymentRequest request) {
        String normalizedIdempotencyKey = paymentCommandValidator.normalizeIdempotencyKey(idempotencyKey);

        return paymentRepository.findByUserIdAndIdempotencyKey(userId, normalizedIdempotencyKey)
                .map(this::toCreateResponse)
                .orElseGet(() -> createNewPayment(userId, normalizedIdempotencyKey, request));
    }

    private CreatePaymentResponse createNewPayment(UUID userId,
                                                   String idempotencyKey,
                                                   CreatePaymentRequest request) {
        paymentRepository.findByBookingId(request.bookingId())
                .ifPresent(paymentCommandValidator::ensurePaymentDoesNotExist);

        Instant now = Instant.now(clock);
        BookingPaymentInfoResponse booking = bookingServiceClient.getPaymentInfo(request.bookingId());
        paymentCommandValidator.validateBookingPayable(userId, booking, now);

        String providerInvoiceId = mockInvoiceService.createProviderInvoiceId();
        PaymentEntity payment = paymentMapper.toPendingPayment(
                userId,
                idempotencyKey,
                booking,
                providerInvoiceId,
                now
        );

        PaymentEntity savedPayment = paymentRepository.save(payment);
        outboxEventRepository.save(outboxMapper.toPaymentCreatedOutbox(savedPayment, now));
        return toCreateResponse(savedPayment);
    }

    private CreatePaymentResponse toCreateResponse(PaymentEntity payment) {
        return paymentMapper.toCreateResponse(
                payment,
                mockInvoiceService.createPaymentUrl(payment.getProviderInvoiceId())
        );
    }

}
