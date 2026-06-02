package com.opspilot.ai;

import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiAiClientTest {

    private final GeminiProperties geminiProperties = new GeminiProperties();
    private final GeminiAiClient geminiAiClient = new GeminiAiClient(geminiProperties, new ObjectMapper());

    @Test
    void parseGeminiResponse_whenProbableCauseIsMissing_shouldUseFallback() {
        geminiProperties.setModel("gemini-test");

        AiTriageOutput output = geminiAiClient.parseGeminiResponse("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"summary\\":\\"Map layer latency reported\\",\\"category\\":\\"performance\\",\\"severity\\":\\"medium\\",\\"recommendedAction\\":\\"Investigate map service latency\\",\\"confidenceScore\\":0.72,\\"requiresHumanReview\\":false}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals("Map layer latency reported", output.summary());
        assertEquals(TicketCategory.PERFORMANCE, output.category());
        assertEquals(TicketSeverity.MEDIUM, output.severity());
        assertEquals("Probable cause could not be determined from the ticket details.", output.probableCause());
        assertEquals("Investigate map service latency", output.recommendedAction());
        assertEquals(0.72, output.confidenceScore());
    }

    @Test
    void parseGeminiResponse_whenConfidenceIsInvalid_shouldUseFallbackConfidence() {
        AiTriageOutput output = geminiAiClient.parseGeminiResponse("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "```json\\n{\\"summary\\":\\"Potential access issue\\",\\"category\\":\\"ACCESS\\",\\"severity\\":\\"HIGH\\",\\"probableCause\\":\\"Permission mismatch\\",\\"recommendedAction\\":\\"Review access policies\\",\\"confidenceScore\\":2.5,\\"requiresHumanReview\\":false}\\n```"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals(0.5, output.confidenceScore());
        assertTrue(output.requiresHumanReview());
    }
}
