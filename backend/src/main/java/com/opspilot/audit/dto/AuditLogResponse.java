package com.opspilot.audit.dto;

import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long ticketId,
        AuditActionType actionType,
        String oldValue,
        String newValue,
        String performedBy,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getTicketId(),
                auditLog.getActionType(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getPerformedBy(),
                auditLog.getCreatedAt()
        );
    }
}