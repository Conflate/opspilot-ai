package com.opspilot.ai.dto;

public record AiTriageRequest(
        String title,
        String description,
        String sourceSystem,
        String affectedService
) {
}