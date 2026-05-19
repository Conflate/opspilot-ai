package com.opspilot.ai;

import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ai.dto.AiTriageRequest;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "opspilot.ai.provider", havingValue = "fake")
public class FakeAiClient implements AiClient {

    @Override
    public AiTriageOutput triageTicket(AiTriageRequest request) {
        String text = (
                request.title() + " " +
                        request.description() + " " +
                        request.sourceSystem() + " " +
                        request.affectedService()
        ).toLowerCase();

        TicketCategory category = determineCategory(text);
        TicketSeverity severity = determineSeverity(text);

        String summary = "The ticket reports: " + request.title();
        String probableCause = determineProbableCause(category);
        String recommendedAction = determineRecommendedAction(category, severity);
        double confidenceScore = determineConfidence(category, severity);
        boolean requiresHumanReview = severity == TicketSeverity.HIGH || severity == TicketSeverity.CRITICAL;

        return new AiTriageOutput(
                summary,
                category,
                severity,
                probableCause,
                recommendedAction,
                confidenceScore,
                requiresHumanReview,
                "fake-ai-v1",
                "{}"
        );
    }

    private TicketCategory determineCategory(String text) {
        if (text.contains("login") || text.contains("access") || text.contains("permission") || text.contains("denied")) {
            return TicketCategory.ACCESS;
        }

        if (text.contains("slow") || text.contains("latency") || text.contains("performance") || text.contains("timeout")) {
            return TicketCategory.PERFORMANCE;
        }

        if (text.contains("config") || text.contains("setting") || text.contains("favourite") || text.contains("favorite")) {
            return TicketCategory.CONFIGURATION;
        }

        if (text.contains("crash") || text.contains("error") || text.contains("exception") || text.contains("bug")) {
            return TicketCategory.BUG;
        }

        if (text.contains("server") || text.contains("database") || text.contains("network") || text.contains("outage")) {
            return TicketCategory.INFRASTRUCTURE;
        }

        if (text.contains("security") || text.contains("breach") || text.contains("unauthorized")) {
            return TicketCategory.SECURITY;
        }

        if (text.contains("data") || text.contains("missing") || text.contains("incorrect")) {
            return TicketCategory.DATA_ISSUE;
        }

        return TicketCategory.OTHER;
    }

    private TicketSeverity determineSeverity(String text) {
        if (text.contains("outage") || text.contains("production down") || text.contains("critical")) {
            return TicketSeverity.CRITICAL;
        }

        if (text.contains("crash") || text.contains("multiple users") || text.contains("cannot access")) {
            return TicketSeverity.HIGH;
        }

        if (text.contains("slow") || text.contains("latency") || text.contains("error")) {
            return TicketSeverity.MEDIUM;
        }

        return TicketSeverity.LOW;
    }

    private String determineProbableCause(TicketCategory category) {
        return switch (category) {
            case ACCESS -> "The issue may be related to permissions, account state, or role-based access.";
            case PERFORMANCE -> "The issue may be related to backend latency, database performance, or network delay.";
            case CONFIGURATION -> "The issue may be related to user-specific settings or configuration synchronization.";
            case BUG -> "The issue may be caused by an application defect or unexpected runtime condition.";
            case INFRASTRUCTURE -> "The issue may be related to server, network, or database infrastructure.";
            case SECURITY -> "The issue may require review for unauthorized access or security policy violations.";
            case DATA_ISSUE -> "The issue may involve missing, stale, or inconsistent operational data.";
            default -> "The ticket does not contain enough information to determine a specific cause.";
        };
    }

    private String determineRecommendedAction(TicketCategory category, TicketSeverity severity) {
        String baseAction = switch (category) {
            case ACCESS -> "Verify the user's account, role, permissions, and recent access changes.";
            case PERFORMANCE -> "Check application logs, database metrics, and recent performance-related changes.";
            case CONFIGURATION -> "Compare user configuration, server-side settings, and recent configuration updates.";
            case BUG -> "Reproduce the issue, inspect logs, and identify the failing component.";
            case INFRASTRUCTURE -> "Check service health, network connectivity, database status, and recent deployments.";
            case SECURITY -> "Escalate for security review and preserve relevant logs.";
            case DATA_ISSUE -> "Validate the affected records, data source, and synchronization process.";
            default -> "Collect more details from the user and assign the ticket for manual review.";
        };

        if (severity == TicketSeverity.CRITICAL || severity == TicketSeverity.HIGH) {
            return baseAction + " Escalate promptly due to severity.";
        }

        return baseAction;
    }

    private double determineConfidence(TicketCategory category, TicketSeverity severity) {
        if (category == TicketCategory.OTHER) {
            return 0.45;
        }

        if (severity == TicketSeverity.CRITICAL || severity == TicketSeverity.HIGH) {
            return 0.80;
        }

        return 0.70;
    }
}