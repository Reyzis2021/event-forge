package com.reyzarium.eventforge.ticketservice.api.ticket;

import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketPdfResponse;
import com.reyzarium.eventforge.ticketservice.api.ticket.dto.TicketResponse;
import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketRequest;
import com.reyzarium.eventforge.ticketservice.api.ticket.dto.ValidateTicketResponse;
import com.reyzarium.eventforge.ticketservice.application.service.TicketCommandService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketDocumentService;
import com.reyzarium.eventforge.ticketservice.application.service.TicketQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketCommandService ticketCommandService;
    private final TicketDocumentService ticketDocumentService;
    private final TicketQueryService ticketQueryService;

    @GetMapping("/my")
    public List<TicketResponse> getMyTickets(@RequestHeader("X-User-Id") UUID userId) {
        return ticketQueryService.getMyTickets(userId);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(@RequestHeader("X-User-Id") UUID userId,
                                    @PathVariable UUID ticketId) {
        return ticketQueryService.getTicket(userId, ticketId);
    }

    @GetMapping("/{ticketId}/pdf")
    public TicketPdfResponse getTicketPdf(@RequestHeader("X-User-Id") UUID userId,
                                          @PathVariable UUID ticketId) {
        return ticketDocumentService.getTicketPdf(userId, ticketId);
    }

    @PostMapping("/validate")
    public ValidateTicketResponse validateTicket(@Valid @RequestBody ValidateTicketRequest request) {
        return ticketCommandService.validateTicket(request);
    }
}
