package com.reyzarium.eventforge.paymentservice.infrastructure.client;

import com.reyzarium.eventforge.paymentservice.common.error.PaymentErrorCode;
import com.reyzarium.eventforge.paymentservice.common.error.PaymentServiceException;
import com.reyzarium.eventforge.paymentservice.infrastructure.client.dto.BookingPaymentInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BookingServiceClient {

    private final RestClient bookingServiceRestClient;

    public BookingPaymentInfoResponse getPaymentInfo(UUID bookingId) {
        try {
            return bookingServiceRestClient
                    .get()
                    .uri("/internal/v1/bookings/{bookingId}/payment-info", bookingId)
                    .retrieve()
                    .body(BookingPaymentInfoResponse.class);
        } catch (RestClientException exception) {
            throw new PaymentServiceException(
                    PaymentErrorCode.BOOKING_SERVICE_UNAVAILABLE,
                    "Booking service is unavailable"
            );
        }
    }
}
