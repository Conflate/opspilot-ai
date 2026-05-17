package com.opspilot.triage.dto;

import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import com.opspilot.triage.AiTriageResult;

import java.time.Instant;

public record TriageResultResponse(
        Long id,
        Long ticketId,
        String summary,
        TicketCategory category,
        TicketSeverity severity,
        String probableCause,
        String recommendedAction,
        Double confidenceScore,
        Boolean requiresHumanReview,
        String modelName,
        Instant createdAt
) {
    public static TriageResultResponse from(AiTriageResult result) {
        return new TriageResultResponse(
                result.getId(),
                result.getTicketId(),
                result.getSummary(),
                result.getCategory(),
                result.getSeverity(),
                result.getProbableCause(),
                result.getRecommendedAction(),
                result.getConfidenceScore(),
                result.getRequiresHumanReview(),
                result.getModelName(),
                result.getCreatedAt()
        );
    }
}