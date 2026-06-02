package com.opspilot.duplicate;

import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.common.InvalidOperationException;
import com.opspilot.common.ResourceNotFoundException;
import com.opspilot.duplicate.dto.DuplicateLinkResponse;
import com.opspilot.duplicate.dto.MarkDuplicateRequest;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DuplicateLinkService {

    private final TicketRepository ticketRepository;
    private final DuplicateLinkRepository duplicateLinkRepository;
    private final AuditService auditService;

    public DuplicateLinkService(
            TicketRepository ticketRepository,
            DuplicateLinkRepository duplicateLinkRepository,
            AuditService auditService
    ) {
        this.ticketRepository = ticketRepository;
        this.duplicateLinkRepository = duplicateLinkRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DuplicateLinkResponse markDuplicate(
            Long sourceTicketId,
            Long duplicateOfTicketId,
            MarkDuplicateRequest request
    ) {
        if (sourceTicketId.equals(duplicateOfTicketId)) {
            throw new InvalidOperationException("A ticket cannot be marked as a duplicate of itself");
        }

        Ticket sourceTicket = ticketRepository.findById(sourceTicketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + sourceTicketId));

        Ticket duplicateOfTicket = ticketRepository.findById(duplicateOfTicketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + duplicateOfTicketId));

        if (duplicateLinkRepository.existsBySourceTicketIdAndDuplicateOfTicketId(sourceTicketId, duplicateOfTicketId)) {
            throw new InvalidOperationException(
                    "Ticket " + sourceTicketId + " is already marked as a duplicate of ticket " + duplicateOfTicketId
            );
        }

        String markedBy = normalizeMarkedBy(request);
        String note = normalizeNote(request);

        DuplicateLink duplicateLink = new DuplicateLink();
        duplicateLink.setSourceTicketId(sourceTicketId);
        duplicateLink.setDuplicateOfTicketId(duplicateOfTicketId);
        duplicateLink.setMarkedBy(markedBy);
        duplicateLink.setNote(note);

        DuplicateLink savedLink = duplicateLinkRepository.save(duplicateLink);

        auditService.log(
                sourceTicketId,
                AuditActionType.DUPLICATE_CHECKED,
                null,
                "Marked as duplicate of ticket #" + duplicateOfTicketId + ": " + duplicateOfTicket.getTitle(),
                markedBy
        );

        auditService.log(
                duplicateOfTicketId,
                AuditActionType.DUPLICATE_CHECKED,
                null,
                "Ticket #" + sourceTicketId + " marked as duplicate: " + sourceTicket.getTitle(),
                markedBy
        );

        return DuplicateLinkResponse.from(savedLink);
    }

    @Transactional(readOnly = true)
    public List<DuplicateLinkResponse> getDuplicateLinks(Long sourceTicketId) {
        if (!ticketRepository.existsById(sourceTicketId)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + sourceTicketId);
        }

        return duplicateLinkRepository.findBySourceTicketIdOrderByCreatedAtDesc(sourceTicketId)
                .stream()
                .map(DuplicateLinkResponse::from)
                .toList();
    }

    private String normalizeMarkedBy(MarkDuplicateRequest request) {
        if (request == null || request.markedBy() == null || request.markedBy().isBlank()) {
            return "operator";
        }

        return request.markedBy().trim();
    }

    private String normalizeNote(MarkDuplicateRequest request) {
        if (request == null || request.note() == null || request.note().isBlank()) {
            return null;
        }

        return request.note().trim();
    }
}
