package com.reyzarium.eventforge.ticketservice.infrastructure.persistence.entity;

import com.reyzarium.eventforge.ticketservice.domain.ticket.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickets")
public class TicketEntity {

    @Id
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "ticket_type_id", nullable = false)
    private UUID ticketTypeId;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "qr_token_hash", nullable = false, unique = true)
    private String qrTokenHash;

    @Column(name = "pdf_file_key")
    private String pdfFileKey;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public boolean isActive() {
        return status == TicketStatus.ACTIVE;
    }

    public boolean isUsed() {
        return status == TicketStatus.USED;
    }

    public void markUsed(Instant now) {
        this.status = TicketStatus.USED;
        this.usedAt = now;
        this.updatedAt = now;
    }

    public void markCancelled(Instant now) {
        this.status = TicketStatus.CANCELLED;
        this.updatedAt = now;
    }
}
