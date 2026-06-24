package com.reyzarium.eventforge.paymentservice.application.mapper;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentResponse;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.PaymentResponse;
import com.reyzarium.eventforge.paymentservice.domain.payment.PaymentStatus;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.dto.BookingPaymentInfoResponse;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, PaymentStatus.class})
public interface PaymentMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "amount", source = "booking.amount")
    @Mapping(target = "currency", source = "booking.currency")
    @Mapping(target = "status", expression = "java(PaymentStatus.PENDING)")
    @Mapping(target = "provider", constant = "MOCK")
    @Mapping(target = "providerInvoiceId", source = "providerInvoiceId")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "failedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    PaymentEntity toPendingPayment(
            UUID userId,
            String idempotencyKey,
            BookingPaymentInfoResponse booking,
            String providerInvoiceId,
            Instant now
    );

    @Mapping(target = "paymentId", source = "payment.id")
    @Mapping(target = "paymentUrl", source = "paymentUrl")
    CreatePaymentResponse toCreateResponse(PaymentEntity payment, String paymentUrl);

    @Mapping(target = "paymentId", source = "id")
    PaymentResponse toResponse(PaymentEntity payment);
}
