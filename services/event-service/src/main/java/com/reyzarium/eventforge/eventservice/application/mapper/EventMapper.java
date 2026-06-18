package com.reyzarium.eventforge.eventservice.application.mapper;

import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventRequest;
import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateTicketTypeRequest;
import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.EventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.TicketTypeEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, EventStatus.class})
public abstract class EventMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "organizerId", source = "organizerId")
    @Mapping(target = "status", expression = "java(EventStatus.DRAFT)")
    @Mapping(target = "capacity", expression = "java(calculateCapacity(request))")
    @Mapping(target = "availableForBooking", constant = "false")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "ticketTypes", ignore = true)
    public abstract EventEntity toDraftEntity(UUID organizerId, CreateEventRequest request, Instant now);

    @AfterMapping
    protected void mapTicketTypes(CreateEventRequest request, Instant now, @MappingTarget EventEntity event) {
        request.ticketTypes()
                .stream()
                .map(ticketTypeRequest -> toTicketTypeEntity(ticketTypeRequest, now))
                .forEach(event::addTicketType);
    }

    protected TicketTypeEntity toTicketTypeEntity(CreateTicketTypeRequest request, Instant now) {
        return TicketTypeEntity.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .price(request.price())
                .currency(request.currency())
                .capacity(request.capacity())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    protected int calculateCapacity(CreateEventRequest request) {
        return request.ticketTypes()
                .stream()
                .mapToInt(CreateTicketTypeRequest::capacity)
                .sum();
    }
}
