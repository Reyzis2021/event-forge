package com.reyzarium.eventforge.bookingservice.application.service;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.BookingResponse;
import com.reyzarium.eventforge.bookingservice.api.internal.dto.BookingPaymentInfoResponse;

import java.util.UUID;

public interface BookingQueryService {

    BookingResponse getBooking(UUID userId, UUID bookingId);

    BookingPaymentInfoResponse getPaymentInfo(UUID bookingId);
}
