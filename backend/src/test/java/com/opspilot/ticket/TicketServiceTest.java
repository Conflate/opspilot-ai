package com.opspilot.ticket;

import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.ticket.dto.CreateTicketRequest;
import com.opspilot.ticket.dto.TicketResponse;
import org.junit.jupiter.api.Test;


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final AuditService auditService = mock(AuditService.class);
    private final TicketService ticketService = new TicketService(ticketRepository, auditService);

    @Test
    void createTicket_shouldCreateTicketAndWriteAuditLog() {
        CreateTicketRequest request = new CreateTicketRequest(
                "Map layer loading slowly",
                "Several users report high latency.",
                "Service Desk",
                "NinJo"
        );

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            setId(ticket, 1L);
            ticket.onCreate();
            return ticket;
        });

        TicketResponse response = ticketService.createTicket(request);

        assertEquals(1L, response.id());
        assertEquals("Map layer loading slowly", response.title());
        assertEquals(TicketStatus.OPEN, response.status());
        assertEquals(TicketSeverity.UNTRIAGED, response.severity());
        assertEquals(TicketCategory.UNCLASSIFIED, response.category());

        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.TICKET_CREATED),
                isNull(),
                contains("Ticket created"),
                eq("system")
        );
    }

    @Test
    void getTickets_shouldReturnAllTickets() {
        Ticket ticket = new Ticket();
        setId(ticket, 1L);
        ticket.setTitle("Test ticket");
        ticket.setDescription("Description");
        ticket.setSourceSystem("Service Desk");
        ticket.setAffectedService("NinJo");
        ticket.onCreate();

        when(ticketRepository.findAll()).thenReturn(List.of(ticket));

        List<TicketResponse> responses = ticketService.getTickets();

        assertEquals(1, responses.size());
        assertEquals("Test ticket", responses.get(0).title());
    }

    @Test
    void getTicket_whenTicketExists_shouldReturnTicket() {
        Ticket ticket = new Ticket();
        setId(ticket, 1L);
        ticket.setTitle("Test ticket");
        ticket.setDescription("Description");
        ticket.setSourceSystem("Service Desk");
        ticket.setAffectedService("NinJo");
        ticket.onCreate();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.getTicket(1L);

        assertEquals(1L, response.id());
        assertEquals("Test ticket", response.title());
    }

    private Ticket newTicket() {
        try {
            var constructor = Ticket.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void setId(Ticket ticket, Long id) {
        try {
            var field = Ticket.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ticket, id);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}