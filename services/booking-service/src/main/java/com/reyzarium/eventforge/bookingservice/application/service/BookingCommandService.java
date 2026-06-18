package com.reyzarium.eventforge.bookingservice.application.service;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingRequest;
import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingResponse;

import java.util.UUID;

public interface BookingCommandService {

    CreateBookingResponse createBooking(UUID userId, String idempotencyKey, CreateBookingRequest request);
}
