package com.opspilot.ticket.dto;

import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import com.opspilot.ticket.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        String sourceSystem,
        String affectedService,
        TicketStatus status,
        TicketSeverity severity,
        TicketCategory category,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getSourceSystem(),
                ticket.getAffectedService(),
                ticket.getStatus(),
                ticket.getSeverity(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
