package com.reyzarium.eventforge.bookingservice.api.booking;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.BookingResponse;
import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingRequest;
import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingResponse;
import com.reyzarium.eventforge.bookingservice.application.service.BookingCommandService;
import com.reyzarium.eventforge.bookingservice.application.service.BookingQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingCommandService bookingCommandService;
    private final BookingQueryService bookingQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateBookingResponse createBooking(@RequestHeader("X-User-Id") UUID userId,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody CreateBookingRequest request) {
        return bookingCommandService.createBooking(userId, idempotencyKey, request);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@RequestHeader("X-User-Id") UUID userId,
                                      @PathVariable UUID bookingId) {
        return bookingQueryService.getBooking(userId, bookingId);
    }
}
