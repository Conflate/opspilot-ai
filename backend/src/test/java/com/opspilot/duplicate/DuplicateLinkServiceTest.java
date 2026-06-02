package com.opspilot.duplicate;

import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.common.InvalidOperationException;
import com.opspilot.duplicate.dto.DuplicateLinkResponse;
import com.opspilot.duplicate.dto.MarkDuplicateRequest;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuplicateLinkServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final DuplicateLinkRepository duplicateLinkRepository = mock(DuplicateLinkRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final DuplicateLinkService duplicateLinkService =
            new DuplicateLinkService(ticketRepository, duplicateLinkRepository, auditService);

    @Test
    void markDuplicate_shouldPersistDuplicateLinkAndWriteAuditLogs() {
        Ticket source = createTicket("Map layer lagging");
        Ticket duplicateOf = createTicket("Map layer latency");
        MarkDuplicateRequest request = new MarkDuplicateRequest("Operator", "Same incident.");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(source));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(duplicateOf));
        when(duplicateLinkRepository.existsBySourceTicketIdAndDuplicateOfTicketId(1L, 2L)).thenReturn(false);
        when(duplicateLinkRepository.save(org.mockito.ArgumentMatchers.any(DuplicateLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DuplicateLinkResponse response = duplicateLinkService.markDuplicate(1L, 2L, request);

        assertEquals(1L, response.sourceTicketId());
        assertEquals(2L, response.duplicateOfTicketId());
        assertEquals("Operator", response.markedBy());
        assertEquals("Same incident.", response.note());

        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.DUPLICATE_CHECKED),
                isNull(),
                eq("Marked as duplicate of ticket #2: Map layer latency"),
                eq("Operator")
        );
        verify(auditService).log(
                eq(2L),
                eq(AuditActionType.DUPLICATE_CHECKED),
                isNull(),
                eq("Ticket #1 marked as duplicate: Map layer lagging"),
                eq("Operator")
        );
    }

    @Test
    void markDuplicate_whenSourceAndTargetAreSame_shouldRejectOperation() {
        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> duplicateLinkService.markDuplicate(1L, 1L, new MarkDuplicateRequest("Operator", null))
        );

        assertEquals("A ticket cannot be marked as a duplicate of itself", exception.getMessage());
    }

    @Test
    void markDuplicate_whenLinkAlreadyExists_shouldRejectOperation() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(createTicket("Map layer lagging")));
        when(ticketRepository.findById(2L)).thenReturn(Optional.of(createTicket("Map layer latency")));
        when(duplicateLinkRepository.existsBySourceTicketIdAndDuplicateOfTicketId(1L, 2L)).thenReturn(true);

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> duplicateLinkService.markDuplicate(1L, 2L, new MarkDuplicateRequest("Operator", null))
        );

        assertEquals("Ticket 1 is already marked as a duplicate of ticket 2", exception.getMessage());
    }

    private Ticket createTicket(String title) {
        return new Ticket(
                title,
                "Several users report latency.",
                "Service Desk",
                "NinJo",
                TicketStatus.OPEN,
                null,
                null
        );
    }
}
