package com.reyzarium.eventforge.eventservice.application.mapper;

import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventListItemResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.EventResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.PageResponse;
import com.reyzarium.eventforge.eventservice.api.publicapi.dto.TicketTypeResponse;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.EventEntity;
import com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity.TicketTypeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface EventResponseMapper {

    @Mapping(target = "eventId", source = "id")
    EventResponse toResponse(EventEntity event);

    @Mapping(target = "ticketTypeId", source = "id")
    TicketTypeResponse toTicketTypeResponse(TicketTypeEntity ticketType);

    @Mapping(target = "eventId", source = "id")
    EventListItemResponse toListItemResponse(EventEntity event);

    default <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
