package com.reyzarium.eventforge.bookingservice.application.service.impl;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.BookingResponse;
import com.reyzarium.eventforge.bookingservice.api.internal.dto.BookingPaymentInfoResponse;
import com.reyzarium.eventforge.bookingservice.application.mapper.BookingResponseMapper;
import com.reyzarium.eventforge.bookingservice.application.service.BookingQueryService;
import com.reyzarium.eventforge.bookingservice.common.error.BookingErrorCode;
import com.reyzarium.eventforge.bookingservice.common.error.BookingServiceException;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingQueryServiceImpl implements BookingQueryService {

    private final BookingRepository bookingRepository;
    private final BookingResponseMapper bookingResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID userId, UUID bookingId) {
        var booking = bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new BookingServiceException(
                        BookingErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found"
                ));

        if (!booking.getUserId().equals(userId)) {
            throw new BookingServiceException(
                    BookingErrorCode.BOOKING_NOT_FOUND,
                    "Booking not found"
            );
        }

        return bookingResponseMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingPaymentInfoResponse getPaymentInfo(UUID bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingServiceException(
                        BookingErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found"
                ));

        return new BookingPaymentInfoResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCurrency(),
                booking.getExpiresAt()
        );
    }
}
