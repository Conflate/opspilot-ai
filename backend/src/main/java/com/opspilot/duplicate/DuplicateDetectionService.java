package com.opspilot.duplicate;

import com.opspilot.audit.AuditActionType;
import com.opspilot.audit.AuditService;
import com.opspilot.duplicate.dto.DuplicateCandidateResponse;
import com.opspilot.ticket.Ticket;
import com.opspilot.ticket.TicketRepository;
import com.opspilot.ticket.TicketStatus;
import org.springframework.stereotype.Service;
import com.opspilot.common.ResourceNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionService {

    private final TicketRepository ticketRepository;
    private final AuditService auditService;

    public DuplicateDetectionService(TicketRepository ticketRepository, AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.auditService = auditService;
    }

    public List<DuplicateCandidateResponse> findDuplicates(Long ticketId) {
        Ticket targetTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        List<TicketStatus> searchableStatuses = List.of(
                TicketStatus.OPEN,
                TicketStatus.PENDING_REVIEW,
                TicketStatus.APPROVED,
                TicketStatus.IN_PROGRESS,
                TicketStatus.WAITING
        );

        List<Ticket> candidates = ticketRepository.findByStatusIn(searchableStatuses)
                .stream()
                .filter(ticket -> !ticket.getId().equals(ticketId))
                .toList();

        List<DuplicateCandidateResponse> results = candidates.stream()
                .map(candidate -> scoreCandidate(targetTicket, candidate))
                .filter(candidate -> candidate.similarityScore() >= 0.40)
                .sorted(Comparator.comparingDouble(DuplicateCandidateResponse::similarityScore).reversed())
                .limit(5)
                .toList();

        auditService.log(
                ticketId,
                AuditActionType.DUPLICATE_CHECKED,
                null,
                "Duplicate check completed. Candidates found: " + results.size(),
                "system"
        );

        return results;
    }

    private DuplicateCandidateResponse scoreCandidate(Ticket target, Ticket candidate) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        if (sameText(target.getAffectedService(), candidate.getAffectedService())) {
            score += 0.25;
            reasons.add("same affected service");
        }

        if (target.getCategory() == candidate.getCategory()) {
            score += 0.20;
            reasons.add("same category");
        }

        if (target.getSeverity() == candidate.getSeverity()) {
            score += 0.10;
            reasons.add("same severity");
        }

        double titleOverlap = keywordOverlap(target.getTitle(), candidate.getTitle());
        if (titleOverlap > 0) {
            double titleScore = titleOverlap * 0.25;
            score += titleScore;
            reasons.add("similar title keywords");
        }

        double descriptionOverlap = keywordOverlap(target.getDescription(), candidate.getDescription());
        if (descriptionOverlap > 0) {
            double descriptionScore = descriptionOverlap * 0.20;
            score += descriptionScore;
            reasons.add("similar description keywords");
        }

        double roundedScore = Math.round(score * 100.0) / 100.0;

        String reason = reasons.isEmpty()
                ? "low similarity"
                : String.join(", ", reasons);

        return new DuplicateCandidateResponse(
                candidate.getId(),
                candidate.getTitle(),
                roundedScore,
                reason
        );
    }

    private boolean sameText(String first, String second) {
        if (first == null || second == null) {
            return false;
        }

        return first.trim().equalsIgnoreCase(second.trim());
    }

    private double keywordOverlap(String first, String second) {
        Set<String> firstWords = normalizeWords(first);
        Set<String> secondWords = normalizeWords(second);

        if (firstWords.isEmpty() || secondWords.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(firstWords);
        intersection.retainAll(secondWords);

        Set<String> union = new HashSet<>(firstWords);
        union.addAll(secondWords);

        return (double) intersection.size() / union.size();
    }

    private Set<String> normalizeWords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        Set<String> stopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "to", "of", "in", "on",
                "for", "with", "by", "is", "are", "was", "were", "when", "after",
                "before", "during", "this", "that", "it", "as", "at", "from"
        );

        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());
    }
}