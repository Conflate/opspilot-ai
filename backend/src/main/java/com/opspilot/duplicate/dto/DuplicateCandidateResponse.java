package com.opspilot.duplicate.dto;

public record DuplicateCandidateResponse(
        Long ticketId,
        String title,
        double similarityScore,
        String reason
) {
}