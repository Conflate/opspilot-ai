package com.opspilot.triage;

import com.opspilot.ai.AiClient;
import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ai.dto.AiTriageRequest;
import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.common.InvalidOperationException;
import com.opspilot.common.ResourceNotFoundException;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketStatus;
import com.opspilot.triage.dto.ManualTriageRequest;
import com.opspilot.triage.dto.TriageResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiTriageService {

    private final TicketRepository ticketRepository;
    private final AiTriageRepository aiTriageRepository;
    private final AiClient aiClient;
    private final AuditService auditService;

    public AiTriageService(
            TicketRepository ticketRepository,
            AiTriageRepository aiTriageRepository,
            AiClient aiClient,
            AuditService auditService
    ) {
        this.ticketRepository = ticketRepository;
        this.aiTriageRepository = aiTriageRepository;
        this.aiClient = aiClient;
        this.auditService = auditService;
    }

    @Transactional
    public TriageResultResponse triageTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        auditService.log(
                ticket.getId(),
                AuditActionType.TRIAGE_REQUESTED,
                null,
                "AI triage requested",
                "system"
        );

        AiTriageRequest request = new AiTriageRequest(
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getSourceSystem(),
                ticket.getAffectedService()
        );

        AiTriageOutput output = aiClient.triageTicket(request);

        AiTriageResult result = new AiTriageResult();
        result.setTicketId(ticket.getId());
        result.setSummary(output.summary());
        result.setCategory(output.category());
        result.setSeverity(output.severity());
        result.setProbableCause(output.probableCause());
        result.setRecommendedAction(output.recommendedAction());
        result.setConfidenceScore(output.confidenceScore());
        result.setRequiresHumanReview(output.requiresHumanReview());
        result.setModelName(output.modelName());
        result.setRawResponse(output.rawResponse());

        AiTriageResult savedResult = aiTriageRepository.save(result);

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.PENDING_REVIEW);
        ticketRepository.save(ticket);

        auditService.log(
                ticket.getId(),
                AuditActionType.AI_TRIAGE_COMPLETED,
                oldStatus.name(),
                "AI triage completed. Ticket moved to PENDING_REVIEW.",
                output.modelName()
        );

        return TriageResultResponse.from(savedResult);
    }

    @Transactional(readOnly = true)
    public List<TriageResultResponse> getTriageResultsForTicket(Long ticketId) {
        return aiTriageRepository.findByTicketIdOrderByCreatedAtDesc(ticketId)
                .stream()
                .map(TriageResultResponse::from)
                .toList();
    }

    @Transactional
    public TriageResultResponse manuallyTriageTicket(Long ticketId, ManualTriageRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.REJECTED) {
            throw new InvalidOperationException("Manual triage is only available after an AI triage rejection");
        }

        String oldValue = "status=" + ticket.getStatus()
                + ", severity=" + ticket.getSeverity()
                + ", category=" + ticket.getCategory();

        AiTriageResult result = new AiTriageResult();
        result.setTicketId(ticket.getId());
        result.setSummary("Manual triage applied after human review.");
        result.setCategory(request.category());
        result.setSeverity(request.severity());
        result.setProbableCause(request.probableCause());
        result.setRecommendedAction(request.recommendedAction());
        result.setConfidenceScore(1.0);
        result.setRequiresHumanReview(false);
        result.setModelName("manual");
        result.setRawResponse("Manual triage by " + request.reviewer());

        AiTriageResult savedResult = aiTriageRepository.save(result);

        ticket.setSeverity(request.severity());
        ticket.setCategory(request.category());
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        String newValue = "status=" + ticket.getStatus()
                + ", severity=" + ticket.getSeverity()
                + ", category=" + ticket.getCategory();

        auditService.log(
                ticket.getId(),
                AuditActionType.TRIAGE_EDITED,
                oldValue,
                newValue + ". Manual triage applied.",
                request.reviewer()
        );

        return TriageResultResponse.from(savedResult);
    }
}
