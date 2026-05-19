package com.opspilot.approval.dto;

import jakarta.validation.constraints.NotBlank;

public record ApprovalRequest(
        @NotBlank String reviewer,
        String reviewNote
) {
}