package com.reyzarium.eventforge.paymentservice.application.service.impl;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.PaymentResponse;
import com.reyzarium.eventforge.paymentservice.application.mapper.PaymentMapper;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentQueryService;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID userId, UUID paymentId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new PaymentServiceException(
                        PaymentErrorCode.PAYMENT_NOT_FOUND,
                        "Payment not found"
                ));
    }
}
