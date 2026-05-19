package com.opspilot.dashboard;

import com.opspilot.dashboard.dto.DashboardResponse;
import com.opspilot.ticket.*;
import com.opspilot.triage.AiTriageRepository;
import com.opspilot.triage.AiTriageResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final AiTriageRepository aiTriageRepository;

    public DashboardService(TicketRepository ticketRepository, AiTriageRepository aiTriageRepository) {
        this.ticketRepository = ticketRepository;
        this.aiTriageRepository = aiTriageRepository;
    }

    public DashboardResponse getDashboardSummary() {
        long totalTickets = ticketRepository.count();

        long openTickets = ticketRepository.countByStatus(TicketStatus.OPEN);
        long pendingReviewTickets = ticketRepository.countByStatus(TicketStatus.PENDING_REVIEW);
        long approvedTickets = ticketRepository.countByStatus(TicketStatus.APPROVED);
        long rejectedTickets = ticketRepository.countByStatus(TicketStatus.REJECTED);
        long criticalTickets = ticketRepository.countBySeverity(TicketSeverity.CRITICAL);

        double averageConfidenceScore = aiTriageRepository.findAll()
                .stream()
                .map(AiTriageResult::getConfidenceScore)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        averageConfidenceScore = Math.round(averageConfidenceScore * 100.0) / 100.0;

        Map<TicketSeverity, Long> ticketsBySeverity = Arrays.stream(TicketSeverity.values())
                .collect(Collectors.toMap(
                        severity -> severity,
                        ticketRepository::countBySeverity
                ));

        Map<TicketCategory, Long> ticketsByCategory = Arrays.stream(TicketCategory.values())
                .collect(Collectors.toMap(
                        category -> category,
                        category -> ticketRepository.findAll()
                                .stream()
                                .filter(ticket -> ticket.getCategory() == category)
                                .count()
                ));

        return new DashboardResponse(
                totalTickets,
                openTickets,
                pendingReviewTickets,
                approvedTickets,
                rejectedTickets,
                criticalTickets,
                averageConfidenceScore,
                ticketsBySeverity,
                ticketsByCategory
        );
    }
}