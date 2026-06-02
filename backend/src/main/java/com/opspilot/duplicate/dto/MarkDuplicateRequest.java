package com.opspilot.duplicate.dto;

public record MarkDuplicateRequest(
        String markedBy,
        String note
) {
}
