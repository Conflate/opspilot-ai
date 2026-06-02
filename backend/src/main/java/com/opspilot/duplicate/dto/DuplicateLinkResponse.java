package com.opspilot.duplicate.dto;

import com.opspilot.duplicate.DuplicateLink;

import java.time.Instant;

public record DuplicateLinkResponse(
        Long id,
        Long sourceTicketId,
        Long duplicateOfTicketId,
        String markedBy,
        String note,
        Instant createdAt
) {
    public static DuplicateLinkResponse from(DuplicateLink duplicateLink) {
        return new DuplicateLinkResponse(
                duplicateLink.getId(),
                duplicateLink.getSourceTicketId(),
                duplicateLink.getDuplicateOfTicketId(),
                duplicateLink.getMarkedBy(),
                duplicateLink.getNote(),
                duplicateLink.getCreatedAt()
        );
    }
}
