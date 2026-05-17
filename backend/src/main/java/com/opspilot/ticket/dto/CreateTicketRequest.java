package com.opspilot.ticket.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String sourceSystem,
        String affectedService
) {
}