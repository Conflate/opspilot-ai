package com.opspilot.triage;

import com.opspilot.ai.AiClient;
import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ai.dto.AiTriageRequest;
import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.common.ResourceNotFoundException;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketSeverity;
import com.opspilot.ticket.TicketStatus;
import com.opspilot.triage.dto.TriageResultResponse;
import com.opspilot.triage.dto.ManualTriageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiTriageServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final AiTriageRepository aiTriageRepository = mock(AiTriageRepository.class);
    private final AiClient aiClient = mock(AiClient.class);
    private final AuditService auditService = mock(AuditService.class);
    private final AiTriageService aiTriageService = new AiTriageService(
            ticketRepository,
            aiTriageRepository,
            aiClient,
            auditService
    );

    @Test
    void triageTicket_whenTicketExists_shouldCreateTriageResultMoveTicketToPendingReviewAndAudit() {
        Ticket ticket = ticket();
        AiTriageOutput output = new AiTriageOutput(
                "Users report slow radar map layer loading.",
                TicketCategory.PERFORMANCE,
                TicketSeverity.MEDIUM,
                "Likely latency in map layer rendering or data retrieval.",
                "Check application logs and recent performance-related changes.",
                0.70,
                false,
                "fake-ai-v1",
                "{}"
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(aiClient.triageTicket(any(AiTriageRequest.class))).thenReturn(output);
        when(aiTriageRepository.save(any(AiTriageResult.class))).thenAnswer(invocation -> {
            AiTriageResult result = invocation.getArgument(0);
            setId(result, 10L);
            result.prePersist();
            return result;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TriageResultResponse response = aiTriageService.triageTicket(1L);

        assertEquals(10L, response.id());
        assertEquals(1L, response.ticketId());
        assertEquals("Users report slow radar map layer loading.", response.summary());
        assertEquals(TicketCategory.PERFORMANCE, response.category());
        assertEquals(TicketSeverity.MEDIUM, response.severity());
        assertEquals("fake-ai-v1", response.modelName());
        assertNotNull(response.createdAt());
        assertEquals(TicketStatus.PENDING_REVIEW, ticket.getStatus());

        ArgumentCaptor<AiTriageRequest> requestCaptor = ArgumentCaptor.forClass(AiTriageRequest.class);
        verify(aiClient).triageTicket(requestCaptor.capture());
        assertEquals("Map layer loading slowly", requestCaptor.getValue().title());
        assertEquals("Several users report high latency.", requestCaptor.getValue().description());
        assertEquals("Service Desk", requestCaptor.getValue().sourceSystem());
        assertEquals("NinJo", requestCaptor.getValue().affectedService());

        ArgumentCaptor<AiTriageResult> resultCaptor = ArgumentCaptor.forClass(AiTriageResult.class);
        verify(aiTriageRepository).save(resultCaptor.capture());
        assertEquals(1L, resultCaptor.getValue().getTicketId());
        assertEquals(TicketCategory.PERFORMANCE, resultCaptor.getValue().getCategory());
        assertEquals(TicketSeverity.MEDIUM, resultCaptor.getValue().getSeverity());
        assertEquals(0.70, resultCaptor.getValue().getConfidenceScore());

        verify(ticketRepository).save(ticket);
        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.TRIAGE_REQUESTED),
                isNull(),
                eq("AI triage requested"),
                eq("system")
        );
        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.AI_TRIAGE_COMPLETED),
                eq(TicketStatus.OPEN.name()),
                eq("AI triage completed. Ticket moved to PENDING_REVIEW."),
                eq("fake-ai-v1")
        );
    }

    @Test
    void triageTicket_whenTicketDoesNotExist_shouldThrowResourceNotFoundException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> aiTriageService.triageTicket(99L)
        );

        assertEquals("Ticket not found with id: 99", exception.getMessage());
        verifyNoInteractions(aiClient, aiTriageRepository, auditService);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void getTriageResultsForTicket_shouldReturnSavedResultsInRepositoryOrder() {
        AiTriageResult result = new AiTriageResult();
        setId(result, 10L);
        result.setTicketId(1L);
        result.setSummary("Summary");
        result.setCategory(TicketCategory.PERFORMANCE);
        result.setSeverity(TicketSeverity.MEDIUM);
        result.setProbableCause("Likely latency.");
        result.setRecommendedAction("Check logs.");
        result.setConfidenceScore(0.70);
        result.setRequiresHumanReview(false);
        result.setModelName("fake-ai-v1");
        result.setRawResponse("{}");
        result.prePersist();

        when(aiTriageRepository.findByTicketIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(result));

        List<TriageResultResponse> responses = aiTriageService.getTriageResultsForTicket(1L);

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).id());
        assertEquals(1L, responses.get(0).ticketId());
        assertEquals("Summary", responses.get(0).summary());
        assertEquals(TicketCategory.PERFORMANCE, responses.get(0).category());
    }

    @Test
    void manuallyTriageTicket_shouldApplyHumanCorrectionCreateResultAndAudit() {
        Ticket ticket = ticket();
        ticket.setStatus(TicketStatus.REJECTED);
        ManualTriageRequest request = new ManualTriageRequest(
                TicketSeverity.HIGH,
                TicketCategory.INFRASTRUCTURE,
                "Database connection pool saturation.",
                "Increase pool capacity and inspect slow queries.",
                "Operator"
        );

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(aiTriageRepository.save(any(AiTriageResult.class))).thenAnswer(invocation -> {
            AiTriageResult result = invocation.getArgument(0);
            setId(result, 20L);
            result.prePersist();
            return result;
        });
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TriageResultResponse response = aiTriageService.manuallyTriageTicket(1L, request);

        assertEquals(20L, response.id());
        assertEquals(TicketSeverity.HIGH, response.severity());
        assertEquals(TicketCategory.INFRASTRUCTURE, response.category());
        assertEquals("Database connection pool saturation.", response.probableCause());
        assertEquals("manual", response.modelName());
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(TicketSeverity.HIGH, ticket.getSeverity());
        assertEquals(TicketCategory.INFRASTRUCTURE, ticket.getCategory());

        verify(auditService).log(
                eq(1L),
                eq(AuditActionType.TRIAGE_EDITED),
                eq("status=REJECTED, severity=UNTRIAGED, category=UNCLASSIFIED"),
                eq("status=IN_PROGRESS, severity=HIGH, category=INFRASTRUCTURE. Manual triage applied."),
                eq("Operator")
        );
    }

    private Ticket ticket() {
        Ticket ticket = new Ticket(
                "Map layer loading slowly",
                "Several users report high latency.",
                "Service Desk",
                "NinJo",
                TicketStatus.OPEN,
                TicketSeverity.UNTRIAGED,
                TicketCategory.UNCLASSIFIED
        );
        setId(ticket, 1L);
        return ticket;
    }

    private void setId(Ticket ticket, Long id) {
        setField(ticket, Ticket.class, "id", id);
    }

    private void setId(AiTriageResult result, Long id) {
        setField(result, AiTriageResult.class, "id", id);
    }

    private void setField(Object target, Class<?> targetClass, String fieldName, Object value) {
        try {
            var field = targetClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
