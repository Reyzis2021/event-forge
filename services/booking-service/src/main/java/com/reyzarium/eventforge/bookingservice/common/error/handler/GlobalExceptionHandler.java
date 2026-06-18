package com.reyzarium.eventforge.bookingservice.common.error.handler;

import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.common.error.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingServiceException.class)
    public ResponseEntity<ErrorResponse> handleBookingServiceException(BookingServiceException exception) {
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

        return new ErrorResponse(
                "VALIDATION_FAILED",
                message,
                Instant.now()
        );
    }

    private HttpStatus resolveStatus(BookingServiceException exception) {
        return switch (exception.getErrorCode()) {
            case BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INSUFFICIENT_TICKETS -> HttpStatus.CONFLICT;
            case EVENT_SERVICE_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
