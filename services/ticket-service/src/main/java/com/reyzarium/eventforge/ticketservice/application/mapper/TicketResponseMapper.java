package com.reyzarium.eventforge.ticketservice.application.mapper;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketResponse;
import com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity.TicketEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketResponseMapper {

    @Mapping(target = "ticketId", source = "id")
    TicketResponse toResponse(TicketEntity ticket);
}
