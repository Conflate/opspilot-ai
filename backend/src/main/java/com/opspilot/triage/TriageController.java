package com.opspilot.triage;

import com.opspilot.triage.dto.ManualTriageRequest;
import com.opspilot.triage.dto.TriageResultResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/triage")
public class TriageController {

    private final AiTriageService aiTriageService;

    public TriageController(AiTriageService aiTriageService) {
        this.aiTriageService = aiTriageService;
    }

    @PostMapping
    public TriageResultResponse triageTicket(@PathVariable Long ticketId) {
        return aiTriageService.triageTicket(ticketId);
    }

    @PostMapping("/manual")
    public TriageResultResponse manuallyTriageTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody ManualTriageRequest request
    ) {
        return aiTriageService.manuallyTriageTicket(ticketId, request);
    }

    @GetMapping
    public List<TriageResultResponse> getTriageResultsForTicket(@PathVariable Long ticketId) {
        return aiTriageService.getTriageResultsForTicket(ticketId);
    }
}
