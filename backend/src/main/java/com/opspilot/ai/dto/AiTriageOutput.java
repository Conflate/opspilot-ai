package com.opspilot.ai.dto;

import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;

public record AiTriageOutput(
        String summary,
        TicketCategory category,
        TicketSeverity severity,
        String probableCause,
        String recommendedAction,
        Double confidenceScore,
        Boolean requiresHumanReview,
        String modelName,
        String rawResponse
) {
}