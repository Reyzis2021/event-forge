package com.reyzarium.eventforge.bookingservice.application.mapper;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.BookingItemResponse;
import com.reyzarium.eventforge.bookingservice.api.booking.dto.BookingResponse;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingResponseMapper {

    @Mapping(target = "bookingId", source = "id")
    BookingResponse toResponse(BookingEntity booking);

    BookingItemResponse toItemResponse(BookingItemEntity item);
}