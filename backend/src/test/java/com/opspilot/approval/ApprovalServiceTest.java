package com.opspilot.approval;

import com.opspilot.approval.dto.ApprovalRequest;
import com.opspilot.approval.dto.ApprovalResponse;
import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketSeverity;
import com.opspilot.ticket.TicketStatus;
import com.opspilot.triage.AiTriageRepository;
import com.opspilot.triage.AiTriageResult;
import com.opspilot.common.InvalidOperationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final AiTriageRepository aiTriageRepository = mock(AiTriageRepository.class);
    private final ApprovalRepository approvalRepository = mock(ApprovalRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final ApprovalService approvalService = new ApprovalService(
            ticketRepository,
            aiTriageRepository,
            approvalRepository,
            auditService
    );

    @Test
    void approveTriage_shouldApplyAiRecommendationToTicketAndWriteAuditLog() {
        Ticket ticket = createTicket(1L);
        ticket.setStatus(TicketStatus.PENDING_REVIEW);

        AiTriageResult triageResult = createTriageResult(10L, 1L);
        ApprovalRequest request = new ApprovalRequest("Operator", "AI recommendation looks correct.");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(aiTriageRepository.findById(10L)).thenReturn(Optional.of(triageResult));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalRepository.save(any(ApprovalDecision.class))).thenAnswer(invocation -> {
            ApprovalDecision decision = invocation.getArgument(0);
            setField(decision, "id", 100L);
            setField(decision, "createdAt", Instant.now());
            return decision;
        });

        ApprovalResponse response = approvalService.approveTriage(1L, 10L, request);

        assertEquals(100L, response.id());
        assertEquals(1L, response.ticketId());
        assertEquals(10L, response.triageResultId());
        assertEquals(ApprovalDecisionType.APPROVED, response.decision());
        assertEquals("Operator", response.reviewer());

        assertEquals(TicketStatus.APPROVED, ticket.getStatus());
        assertEquals(TicketSeverity.HIGH, ticket.getSeverity());
        assertEquals(TicketCategory.PERFORMANCE, ticket.getCategory());

        verify(ticketRepository).save(ticket);
        verify(approvalRepository).save(any(ApprovalDecision.class));
        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.TRIAGE_APPROVED),
                contains("status=PENDING_REVIEW"),
                contains("status=APPROVED"),
                eq("Operator")
        );
    }

    @Test
    void rejectTriage_shouldNotApplyAiRecommendationAndShouldWriteAuditLog() {
        Ticket ticket = createTicket(1L);
        ticket.setStatus(TicketStatus.PENDING_REVIEW);

        AiTriageResult triageResult = createTriageResult(10L, 1L);
        ApprovalRequest request = new ApprovalRequest("Operator", "AI recommendation is not accurate.");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(aiTriageRepository.findById(10L)).thenReturn(Optional.of(triageResult));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(approvalRepository.save(any(ApprovalDecision.class))).thenAnswer(invocation -> {
            ApprovalDecision decision = invocation.getArgument(0);
            setField(decision, "id", 101L);
            setField(decision, "createdAt", Instant.now());
            return decision;
        });

        ApprovalResponse response = approvalService.rejectTriage(1L, 10L, request);

        assertEquals(101L, response.id());
        assertEquals(1L, response.ticketId());
        assertEquals(10L, response.triageResultId());
        assertEquals(ApprovalDecisionType.REJECTED, response.decision());
        assertEquals("Operator", response.reviewer());

        assertEquals(TicketStatus.REJECTED, ticket.getStatus());
        assertEquals(TicketSeverity.UNTRIAGED, ticket.getSeverity());
        assertEquals(TicketCategory.UNCLASSIFIED, ticket.getCategory());

        verify(ticketRepository).save(ticket);
        verify(approvalRepository).save(any(ApprovalDecision.class));
        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.TRIAGE_REJECTED),
                eq("PENDING_REVIEW"),
                contains("AI triage rejected"),
                eq("Operator")
        );
    }

    @Test
    void approveTriage_whenTicketIsNotPendingReview_shouldRejectOperation() {
        Ticket ticket = createTicket(1L);
        ticket.setStatus(TicketStatus.OPEN);
        AiTriageResult triageResult = createTriageResult(10L, 1L);
        ApprovalRequest request = new ApprovalRequest("Operator", "Looks good.");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(aiTriageRepository.findById(10L)).thenReturn(Optional.of(triageResult));

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> approvalService.approveTriage(1L, 10L, request)
        );

        assertEquals("Ticket must be PENDING_REVIEW before triage can be approved or rejected", exception.getMessage());
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(approvalRepository, never()).save(any(ApprovalDecision.class));
    }

    @Test
    void approveTriage_whenTriageAlreadyReviewed_shouldRejectOperation() {
        Ticket ticket = createTicket(1L);
        ticket.setStatus(TicketStatus.PENDING_REVIEW);
        AiTriageResult triageResult = createTriageResult(10L, 1L);
        ApprovalRequest request = new ApprovalRequest("Operator", "Looks good.");

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(aiTriageRepository.findById(10L)).thenReturn(Optional.of(triageResult));
        when(approvalRepository.existsByTriageResultId(10L)).thenReturn(true);

        InvalidOperationException exception = assertThrows(
                InvalidOperationException.class,
                () -> approvalService.approveTriage(1L, 10L, request)
        );

        assertEquals("AI triage result has already been reviewed: 10", exception.getMessage());
        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(approvalRepository, never()).save(any(ApprovalDecision.class));
    }

    private Ticket createTicket(Long id) {
        Ticket ticket = newTicket();
        setField(ticket, "id", id);
        ticket.setTitle("Map layer loading slowly");
        ticket.setDescription("Several users report high latency.");
        ticket.setSourceSystem("Service Desk");
        ticket.setAffectedService("NinJo");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setSeverity(TicketSeverity.UNTRIAGED);
        ticket.setCategory(TicketCategory.UNCLASSIFIED);
        return ticket;
    }

    private AiTriageResult createTriageResult(Long id, Long ticketId) {
        AiTriageResult result = new AiTriageResult();
        setField(result, "id", id);
        result.setTicketId(ticketId);
        result.setSummary("The ticket reports slow map layer loading.");
        result.setCategory(TicketCategory.PERFORMANCE);
        result.setSeverity(TicketSeverity.HIGH);
        result.setProbableCause("Possible backend latency.");
        result.setRecommendedAction("Check logs and recent map service changes.");
        result.setConfidenceScore(0.8);
        result.setRequiresHumanReview(true);
        result.setModelName("fake-ai-v1");
        result.setRawResponse("{}");
        setField(result, "createdAt", Instant.now());
        return result;
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
