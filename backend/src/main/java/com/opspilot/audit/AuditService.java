package com.opspilot.audit;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void log(Long ticketId, AuditActionType actionType, String oldValue, String newValue, String performedBy) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTicketId(ticketId);
        auditLog.setActionType(actionType);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setPerformedBy(performedBy);

        auditRepository.save(auditLog);
    }

    public List<AuditLog> getAuditLogsForTicket(Long ticketId) {
        return auditRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }
}