package com.opspilot.duplicate;

import com.opspilot.duplicate.dto.DuplicateCandidateResponse;
import com.opspilot.duplicate.dto.DuplicateLinkResponse;
import com.opspilot.duplicate.dto.MarkDuplicateRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/duplicates")
public class DuplicateController {

    private final DuplicateDetectionService duplicateDetectionService;
    private final DuplicateLinkService duplicateLinkService;

    public DuplicateController(
            DuplicateDetectionService duplicateDetectionService,
            DuplicateLinkService duplicateLinkService
    ) {
        this.duplicateDetectionService = duplicateDetectionService;
        this.duplicateLinkService = duplicateLinkService;
    }

    @GetMapping
    public List<DuplicateCandidateResponse> findDuplicates(@PathVariable Long ticketId) {
        return duplicateDetectionService.findDuplicates(ticketId);
    }

    @GetMapping("/links")
    public List<DuplicateLinkResponse> getDuplicateLinks(@PathVariable Long ticketId) {
        return duplicateLinkService.getDuplicateLinks(ticketId);
    }

    @PostMapping("/{duplicateOfTicketId}")
    public DuplicateLinkResponse markDuplicate(
            @PathVariable Long ticketId,
            @PathVariable Long duplicateOfTicketId,
            @RequestBody(required = false) MarkDuplicateRequest request
    ) {
        return duplicateLinkService.markDuplicate(ticketId, duplicateOfTicketId, request);
    }
}
