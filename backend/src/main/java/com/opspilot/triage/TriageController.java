package com.opspilot.triage;

import com.opspilot.triage.dto.TriageResultResponse;
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

    @GetMapping
    public List<TriageResultResponse> getTriageResultsForTicket(@PathVariable Long ticketId) {
        return aiTriageService.getTriageResultsForTicket(ticketId);
    }
}