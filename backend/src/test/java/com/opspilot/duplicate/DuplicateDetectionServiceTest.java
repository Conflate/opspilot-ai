package com.opspilot.duplicate;

import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.duplicate.dto.DuplicateCandidateResponse;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketSeverity;
import com.opspilot.ticket.TicketStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuplicateDetectionServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final DuplicateDetectionService duplicateDetectionService =
            new DuplicateDetectionService(ticketRepository, auditService);

    @Test
    void findDuplicates_shouldReturnSimilarTicketsAndWriteAuditLog() {
        Ticket target = createTicket(
                1L,
                "Map layer loading slowly",
                "Several users report high latency when loading radar map layers.",
                "NinJo",
                TicketStatus.PENDING_REVIEW,
                TicketSeverity.MEDIUM,
                TicketCategory.PERFORMANCE
        );

        Ticket similar = createTicket(
                2L,
                "Radar map layers are slow",
                "Users are reporting slow loading and latency with radar map layers.",
                "NinJo",
                TicketStatus.OPEN,
                TicketSeverity.MEDIUM,
                TicketCategory.PERFORMANCE
        );

        Ticket unrelated = createTicket(
                3L,
                "User cannot access admin panel",
                "Access denied when opening configuration settings.",
                "Admin Tool",
                TicketStatus.OPEN,
                TicketSeverity.LOW,
                TicketCategory.ACCESS
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(target));
        when(ticketRepository.findByStatusIn(anyList())).thenReturn(List.of(target, similar, unrelated));

        List<DuplicateCandidateResponse> results = duplicateDetectionService.findDuplicates(1L);

        assertFalse(results.isEmpty());
        assertEquals(2L, results.get(0).ticketId());
        assertEquals("Radar map layers are slow", results.get(0).title());

        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.DUPLICATE_CHECKED),
                isNull(),
                eq("Duplicate check completed. Candidates found: " + results.size()),
                eq("system")
        );
    }

    private Ticket createTicket(
            Long id,
            String title,
            String description,
            String affectedService,
            TicketStatus status,
            TicketSeverity severity,
            TicketCategory category
    ) {
        Ticket ticket = newTicket();
        setField(ticket, "id", id);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setSourceSystem("Service Desk");
        ticket.setAffectedService(affectedService);
        ticket.setStatus(status);
        ticket.setSeverity(severity);
        ticket.setCategory(category);
        return ticket;
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

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
