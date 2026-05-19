package com.opspilot.duplicate;

import com.opspilot.duplicate.dto.DuplicateCandidateResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/duplicates")
public class DuplicateController {

    private final DuplicateDetectionService duplicateDetectionService;

    public DuplicateController(DuplicateDetectionService duplicateDetectionService) {
        this.duplicateDetectionService = duplicateDetectionService;
    }

    @GetMapping
    public List<DuplicateCandidateResponse> findDuplicates(@PathVariable Long ticketId) {
        return duplicateDetectionService.findDuplicates(ticketId);
    }
}