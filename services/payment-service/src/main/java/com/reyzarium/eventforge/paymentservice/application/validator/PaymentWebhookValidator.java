package com.reyzarium.eventforge.paymentservice.application.validator;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.MockPaymentWebhookRequest;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookValidator {

    public static final String SUCCEEDED_STATUS = "SUCCEEDED";
    public static final String FAILED_STATUS = "FAILED";

    public void validate(MockPaymentWebhookRequest request, PaymentEntity payment) {
        if (!isSucceededStatus(request.status()) && !isFailedStatus(request.status())) {
            throw new PaymentServiceException(
                    PaymentErrorCode.WEBHOOK_UNSUPPORTED_STATUS,
                    "Unsupported mock webhook status"
            );
        }

        if (isSucceededStatus(request.status()) && payment.isFailed()) {
            throw new PaymentServiceException(
                    PaymentErrorCode.PAYMENT_ALREADY_FAILED,
                    "Failed payment cannot be marked as succeeded"
            );
        }

        if (isFailedStatus(request.status()) && payment.isSucceeded()) {
            throw new PaymentServiceException(
                    PaymentErrorCode.PAYMENT_ALREADY_SUCCEEDED,
                    "Succeeded payment cannot be marked as failed"
            );
        }

        if (request.amount().compareTo(payment.getAmount()) != 0 || !request.currency().equals(payment.getCurrency())) {
            throw new PaymentServiceException(
                    PaymentErrorCode.WEBHOOK_INVALID_AMOUNT,
                    "Webhook amount or currency does not match payment"
            );
        }
    }

    public boolean isSucceededStatus(String status) {
        return SUCCEEDED_STATUS.equals(status);
    }

    public boolean isFailedStatus(String status) {
        return FAILED_STATUS.equals(status);
    }
}
