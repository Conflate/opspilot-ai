package com.opspilot.ticket;

import com.opspilot.ticket.dto.CreateTicketRequest;
import com.opspilot.ticket.dto.TicketResponse;
import org.springframework.stereotype.Service;
import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AuditService auditService;

    public TicketService(TicketRepository ticketRepository, AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.auditService = auditService;
    }

    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setSourceSystem(request.sourceSystem());
        ticket.setAffectedService(request.affectedService());

        Ticket savedTicket = ticketRepository.save(ticket);

        auditService.log(
                savedTicket.getId(),
                AuditActionType.TICKET_CREATED,
                null,
                "Ticket created: " + savedTicket.getTitle(),
                "system"
        );
        return TicketResponse.from(savedTicket);
    }

    public List<TicketResponse> getTickets() {
        return ticketRepository.findAll()
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    public TicketResponse getTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

        return TicketResponse.from(ticket);
    }
}