package com.opspilot.dashboard.dto;

import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;

import java.util.Map;

public record DashboardResponse(
        long totalTickets,
        long openTickets,
        long pendingReviewTickets,
        long approvedTickets,
        long rejectedTickets,
        long criticalTickets,
        double averageConfidenceScore,
        Map<TicketSeverity, Long> ticketsBySeverity,
        Map<TicketCategory, Long> ticketsByCategory
) {
}