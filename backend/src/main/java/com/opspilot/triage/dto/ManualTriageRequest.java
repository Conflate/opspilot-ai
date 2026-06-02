package com.opspilot.triage.dto;

import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualTriageRequest(
        @NotNull TicketSeverity severity,
        @NotNull TicketCategory category,
        @NotBlank String probableCause,
        @NotBlank String recommendedAction,
        @NotBlank String reviewer
) {
}
