package com.reyzarium.eventforge.bookingservice.common.error;

import lombok.Getter;

@Getter
public class BookingServiceException extends RuntimeException {

    private final BookingErrorCode errorCode;

    public BookingServiceException(BookingErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}