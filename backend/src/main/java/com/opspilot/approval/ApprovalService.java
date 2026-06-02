package com.opspilot.approval;

import com.opspilot.approval.dto.ApprovalRequest;
import com.opspilot.approval.dto.ApprovalResponse;
import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.common.InvalidOperationException;
import com.opspilot.common.ResourceNotFoundException;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketStatus;
import com.opspilot.triage.AiTriageRepository;
import com.opspilot.triage.AiTriageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalService {

    private final TicketRepository ticketRepository;
    private final AiTriageRepository aiTriageRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;

    public ApprovalService(
            TicketRepository ticketRepository,
            AiTriageRepository aiTriageRepository,
            ApprovalRepository approvalRepository,
            AuditService auditService
    ) {
        this.ticketRepository = ticketRepository;
        this.aiTriageRepository = aiTriageRepository;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ApprovalResponse approveTriage(Long ticketId, Long triageId, ApprovalRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        AiTriageResult triageResult = aiTriageRepository.findById(triageId)
                .orElseThrow(() -> new ResourceNotFoundException("AI triage result not found with id: " + triageId));

        validateTriageBelongsToTicket(ticketId, triageResult);
        validateTicketPendingReview(ticket);
        validateTriageNotAlreadyDecided(triageId);

        String oldValue = "status=" + ticket.getStatus()
                + ", severity=" + ticket.getSeverity()
                + ", category=" + ticket.getCategory();

        ticket.setSeverity(triageResult.getSeverity());
        ticket.setCategory(triageResult.getCategory());
        ticket.setStatus(TicketStatus.APPROVED);
        ticketRepository.save(ticket);

        ApprovalDecision approvalDecision = new ApprovalDecision();
        approvalDecision.setTicketId(ticketId);
        approvalDecision.setTriageResultId(triageId);
        approvalDecision.setDecision(ApprovalDecisionType.APPROVED);
        approvalDecision.setReviewer(request.reviewer());
        approvalDecision.setReviewNote(request.reviewNote());

        ApprovalDecision savedDecision = approvalRepository.save(approvalDecision);

        String newValue = "status=" + ticket.getStatus()
                + ", severity=" + ticket.getSeverity()
                + ", category=" + ticket.getCategory();

        auditService.log(
                ticketId,
                AuditActionType.TRIAGE_APPROVED,
                oldValue,
                newValue,
                request.reviewer()
        );

        return ApprovalResponse.from(savedDecision);
    }

    @Transactional
    public ApprovalResponse rejectTriage(Long ticketId, Long triageId, ApprovalRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        AiTriageResult triageResult = aiTriageRepository.findById(triageId)
                .orElseThrow(() -> new ResourceNotFoundException("AI triage result not found with id: " + triageId));

        validateTriageBelongsToTicket(ticketId, triageResult);
        validateTicketPendingReview(ticket);
        validateTriageNotAlreadyDecided(triageId);

        TicketStatus oldStatus = ticket.getStatus();

        ticket.setStatus(TicketStatus.REJECTED);
        ticketRepository.save(ticket);

        ApprovalDecision approvalDecision = new ApprovalDecision();
        approvalDecision.setTicketId(ticketId);
        approvalDecision.setTriageResultId(triageId);
        approvalDecision.setDecision(ApprovalDecisionType.REJECTED);
        approvalDecision.setReviewer(request.reviewer());
        approvalDecision.setReviewNote(request.reviewNote());

        ApprovalDecision savedDecision = approvalRepository.save(approvalDecision);

        auditService.log(
                ticketId,
                AuditActionType.TRIAGE_REJECTED,
                oldStatus.name(),
                "AI triage rejected. Ticket moved to REJECTED.",
                request.reviewer()
        );

        return ApprovalResponse.from(savedDecision);
    }

    private void validateTriageBelongsToTicket(Long ticketId, AiTriageResult triageResult) {
        if (!triageResult.getTicketId().equals(ticketId)) {
            throw new InvalidOperationException("Triage result does not belong to ticket id: " + ticketId);
        }
    }

    private void validateTicketPendingReview(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.PENDING_REVIEW) {
            throw new InvalidOperationException("Ticket must be PENDING_REVIEW before triage can be approved or rejected");
        }
    }

    private void validateTriageNotAlreadyDecided(Long triageId) {
        if (approvalRepository.existsByTriageResultId(triageId)) {
            throw new InvalidOperationException("AI triage result has already been reviewed: " + triageId);
        }
    }
}
