package com.reyzarium.eventforge.ticketservice.application.mapper;

import com.reyzarium.eventforge.ticketservice.application.event.BookingConfirmedEvent;
import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, TicketStatus.class})
public interface TicketMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "bookingId", source = "event.bookingId")
    @Mapping(target = "userId", source = "event.userId")
    @Mapping(target = "eventId", source = "event.businessEventId")
    @Mapping(target = "ticketTypeId", source = "event.ticketTypeId")
    @Mapping(target = "ticketNumber", source = "ticketNumber")
    @Mapping(target = "status", expression = "java(TicketStatus.ACTIVE)")
    @Mapping(target = "qrTokenHash", source = "qrTokenHash")
    @Mapping(target = "pdfFileKey", ignore = true)
    @Mapping(target = "issuedAt", source = "now")
    @Mapping(target = "usedAt", ignore = true)
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "version", constant = "0L")
    TicketEntity toActiveTicket(
            BookingConfirmedEvent event,
            String ticketNumber,
            String qrTokenHash,
            Instant now
    );
}
