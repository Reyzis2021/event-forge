package com.reyzarium.eventforge.eventservice.infrastructure.persistence.entity;

import com.reyzarium.eventforge.eventservice.domain.event.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
public class EventEntity {

    @Id
    private UUID id;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "available_for_booking", nullable = false)
    private Boolean availableForBooking;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketTypeEntity> ticketTypes = new ArrayList<>();

    public void addTicketType(TicketTypeEntity ticketType) {
        ticketTypes.add(ticketType);
        ticketType.setEvent(this);
    }

    public void replaceTicketTypes(List<TicketTypeEntity> newTicketTypes) {
        ticketTypes.clear();
        newTicketTypes.forEach(this::addTicketType);
    }
}
