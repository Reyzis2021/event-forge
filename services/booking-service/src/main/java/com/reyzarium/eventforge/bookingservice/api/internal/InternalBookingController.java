package com.reyzarium.eventforge.bookingservice.api.internal;

import com.reyzarium.eventforge.bookingservice.api.internal.dto.BookingPaymentInfoResponse;
import com.reyzarium.eventforge.bookingservice.application.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/bookings")
public class InternalBookingController {

    private final BookingQueryService bookingQueryService;

    @GetMapping("/{bookingId}/payment-info")
    public BookingPaymentInfoResponse getPaymentInfo(@PathVariable UUID bookingId) {
        return bookingQueryService.getPaymentInfo(bookingId);
    }
}
