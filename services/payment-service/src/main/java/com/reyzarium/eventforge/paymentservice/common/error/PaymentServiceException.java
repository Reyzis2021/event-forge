package com.reyzarium.eventforge.paymentservice.common.error;

import lombok.Getter;

@Getter
public class PaymentServiceException extends RuntimeException {

    private final PaymentErrorCode errorCode;

    public PaymentServiceException(PaymentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
