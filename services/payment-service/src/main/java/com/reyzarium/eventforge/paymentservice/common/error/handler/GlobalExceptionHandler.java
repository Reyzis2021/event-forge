package com.reyzarium.eventforge.paymentservice.common.error.handler;

import com.reyzarium.eventforge.paymentservice.common.error.ErrorResponse;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentServiceException.class)
    public ResponseEntity<ErrorResponse> handlePaymentServiceException(PaymentServiceException exception) {
        return ResponseEntity.status(resolveStatus(exception))
                .body(new ErrorResponse(
                        exception.getErrorCode().name(),
                        exception.getMessage(),
                        Instant.now()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return new ErrorResponse("VALIDATION_FAILED", message, Instant.now());
    }

    private HttpStatus resolveStatus(PaymentServiceException exception) {
        return switch (exception.getErrorCode()) {
            case PAYMENT_NOT_FOUND, BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PAYMENT_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case BOOKING_SERVICE_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
