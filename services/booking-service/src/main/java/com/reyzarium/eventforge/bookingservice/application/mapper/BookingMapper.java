package com.reyzarium.eventforge.bookingservice.application.mapper;

import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingRequest;
import com.reyzarium.eventforge.bookingservice.api.booking.dto.CreateBookingResponse;
import com.reyzarium.eventforge.bookingservice.domain.booking.BookingStatus;
import com.reyzarium.eventforge.bookingservice.infrastructure.client.dto.TicketTypeDetailsResponse;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingEntity;
import com.reyzarium.eventforge.bookingservice.infrastructure.persistence.entity.BookingItemEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, BookingStatus.class})
public abstract class BookingMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "eventId", source = "request.eventId")
    @Mapping(target = "idempotencyKey", source = "idempotencyKey")
    @Mapping(target = "status", expression = "java(BookingStatus.PENDING_PAYMENT)")
    @Mapping(target = "totalAmount", expression = "java(calculateTotalAmount(request, ticketType))")
    @Mapping(target = "currency", source = "ticketType.currency")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "items", ignore = true)
    public abstract BookingEntity toPendingPaymentBooking(
            UUID userId,
            String idempotencyKey,
            CreateBookingRequest request,
            TicketTypeDetailsResponse ticketType,
            Instant now,
            Instant expiresAt
    );

    @AfterMapping
    protected void addBookingItem(CreateBookingRequest request,
                                  TicketTypeDetailsResponse ticketType,
                                  Instant now,
                                  @MappingTarget BookingEntity booking) {
        booking.addItem(toBookingItem(request, ticketType, now));
    }

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "booking", ignore = true)
    @Mapping(target = "ticketTypeId", source = "request.ticketTypeId")
    @Mapping(target = "quantity", source = "request.quantity")
    @Mapping(target = "unitPrice", source = "ticketType.price")
    @Mapping(target = "currency", source = "ticketType.currency")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "version", ignore = true)
    protected abstract BookingItemEntity toBookingItem(
            CreateBookingRequest request,
            TicketTypeDetailsResponse ticketType,
            Instant now
    );

    @Mapping(target = "bookingId", source = "id")
    @Mapping(target = "amount", source = "totalAmount")
    public abstract CreateBookingResponse toCreateResponse(BookingEntity booking);

    protected BigDecimal calculateTotalAmount(CreateBookingRequest request, TicketTypeDetailsResponse ticketType) {
        return ticketType.price().multiply(BigDecimal.valueOf(request.quantity()));
    }
}
