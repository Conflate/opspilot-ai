package com.opspilot.dashboard;

import com.opspilot.dashboard.dto.DashboardResponse;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketSeverity;
import com.opspilot.ticket.TicketStatus;
import com.opspilot.triage.AiTriageRepository;
import com.opspilot.triage.AiTriageResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final TicketRepository ticketRepository = mock(TicketRepository.class);
    private final AiTriageRepository aiTriageRepository = mock(AiTriageRepository.class);

    private final DashboardService dashboardService = new DashboardService(
            ticketRepository,
            aiTriageRepository
    );

    @Test
    void getDashboardSummary_shouldReturnTicketAndTriageMetrics() {
        Ticket performanceTicket = createTicket(
                1L,
                TicketStatus.APPROVED,
                TicketSeverity.HIGH,
                TicketCategory.PERFORMANCE
        );

        Ticket unclassifiedTicket = createTicket(
                2L,
                TicketStatus.OPEN,
                TicketSeverity.UNTRIAGED,
                TicketCategory.UNCLASSIFIED
        );

        AiTriageResult firstTriageResult = createTriageResult(1L, 1L, 0.8);
        AiTriageResult secondTriageResult = createTriageResult(2L, 2L, 0.6);

        when(ticketRepository.count()).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.PENDING_REVIEW)).thenReturn(0L);
        when(ticketRepository.countByStatus(TicketStatus.APPROVED)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.REJECTED)).thenReturn(0L);
        when(ticketRepository.countBySeverity(TicketSeverity.CRITICAL)).thenReturn(0L);

        when(ticketRepository.countBySeverity(TicketSeverity.UNTRIAGED)).thenReturn(1L);
        when(ticketRepository.countBySeverity(TicketSeverity.LOW)).thenReturn(0L);
        when(ticketRepository.countBySeverity(TicketSeverity.MEDIUM)).thenReturn(0L);
        when(ticketRepository.countBySeverity(TicketSeverity.HIGH)).thenReturn(1L);

        when(ticketRepository.countByCategory(TicketCategory.PERFORMANCE)).thenReturn(1L);
        when(ticketRepository.countByCategory(TicketCategory.UNCLASSIFIED)).thenReturn(1L);
        when(aiTriageRepository.findAll()).thenReturn(List.of(firstTriageResult, secondTriageResult));

        DashboardResponse response = dashboardService.getDashboardSummary();

        assertEquals(2L, response.totalTickets());
        assertEquals(1L, response.openTickets());
        assertEquals(0L, response.pendingReviewTickets());
        assertEquals(1L, response.approvedTickets());
        assertEquals(0L, response.rejectedTickets());
        assertEquals(0L, response.criticalTickets());
        assertEquals(0.7, response.averageConfidenceScore());

        assertEquals(1L, response.ticketsBySeverity().get(TicketSeverity.UNTRIAGED));
        assertEquals(1L, response.ticketsBySeverity().get(TicketSeverity.HIGH));
        assertEquals(1L, response.ticketsByCategory().get(TicketCategory.PERFORMANCE));
        assertEquals(1L, response.ticketsByCategory().get(TicketCategory.UNCLASSIFIED));
    }

    private Ticket createTicket(
            Long id,
            TicketStatus status,
            TicketSeverity severity,
            TicketCategory category
    ) {
        Ticket ticket = newTicket();
        setField(ticket, "id", id);
        ticket.setTitle("Test ticket " + id);
        ticket.setDescription("Test description " + id);
        ticket.setSourceSystem("Service Desk");
        ticket.setAffectedService("NinJo");
        ticket.setStatus(status);
        ticket.setSeverity(severity);
        ticket.setCategory(category);
        return ticket;
    }

    private AiTriageResult createTriageResult(Long id, Long ticketId, double confidenceScore) {
        AiTriageResult result = new AiTriageResult();
        setField(result, "id", id);
        result.setTicketId(ticketId);
        result.setSummary("Summary " + id);
        result.setCategory(TicketCategory.PERFORMANCE);
        result.setSeverity(TicketSeverity.HIGH);
        result.setProbableCause("Possible cause.");
        result.setRecommendedAction("Recommended action.");
        result.setConfidenceScore(confidenceScore);
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
