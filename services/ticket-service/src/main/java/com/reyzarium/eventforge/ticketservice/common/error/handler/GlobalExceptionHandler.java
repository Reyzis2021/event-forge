package com.reyzarium.eventforge.ticketservice.common.error.handler;

import com.reyzarium.eventforge.ticketservice.common.error.ErrorResponse;
import com.reyzarium.eventforge.ticketservice.common.error.TicketServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketServiceException.class)
    public ResponseEntity<ErrorResponse> handleTicketServiceException(TicketServiceException exception) {
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

    private HttpStatus resolveStatus(TicketServiceException exception) {
        return switch (exception.getErrorCode()) {
            case TICKET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TICKET_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case TICKET_ISSUANCE_ALREADY_PROCESSED -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
