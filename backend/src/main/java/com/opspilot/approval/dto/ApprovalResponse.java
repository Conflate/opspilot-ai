package com.opspilot.approval.dto;

import com.opspilot.approval.ApprovalDecision;
import com.opspilot.approval.ApprovalDecisionType;

import java.time.Instant;

public record ApprovalResponse(
        Long id,
        Long ticketId,
        Long triageResultId,
        ApprovalDecisionType decision,
        String reviewer,
        String reviewNote,
        Instant createdAt
) {
    public static ApprovalResponse from(ApprovalDecision approvalDecision) {
        return new ApprovalResponse(
                approvalDecision.getId(),
                approvalDecision.getTicketId(),
                approvalDecision.getTriageResultId(),
                approvalDecision.getDecision(),
                approvalDecision.getReviewer(),
                approvalDecision.getReviewNote(),
                approvalDecision.getCreatedAt()
        );
    }
}