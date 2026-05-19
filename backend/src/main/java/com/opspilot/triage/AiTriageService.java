package com.opspilot.triage;

import com.opspilot.ai.AiClient;
import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ai.dto.AiTriageRequest;
import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketStatus;
import com.opspilot.triage.dto.TriageResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found with id: " + ticketId));

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
}
