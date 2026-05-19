package com.opspilot.approval;

import com.opspilot.approval.dto.ApprovalRequest;
import com.opspilot.approval.dto.ApprovalResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets/{ticketId}/triage/{triageId}")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/approve")
    public ApprovalResponse approveTriage(
            @PathVariable Long ticketId,
            @PathVariable Long triageId,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return approvalService.approveTriage(ticketId, triageId, request);
    }

    @PostMapping("/reject")
    public ApprovalResponse rejectTriage(
            @PathVariable Long ticketId,
            @PathVariable Long triageId,
            @Valid @RequestBody ApprovalRequest request
    ) {
        return approvalService.rejectTriage(ticketId, triageId, request);
    }
}