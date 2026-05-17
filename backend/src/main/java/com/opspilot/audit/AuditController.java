package com.opspilot.audit;

import com.opspilot.audit.dto.AuditLogResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/audit-logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public List<AuditLogResponse> getAuditLogsForTicket(@PathVariable Long ticketId) {
        return auditService.getAuditLogsForTicket(ticketId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}