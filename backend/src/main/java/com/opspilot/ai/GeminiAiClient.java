package com.opspilot.ai;

import com.opspilot.ai.dto.AiTriageOutput;
import com.opspilot.ai.dto.AiTriageRequest;
import com.opspilot.common.InvalidOperationException;
import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "opspilot.ai.provider", havingValue = "gemini")
public class GeminiAiClient implements AiClient {

    private final GeminiProperties geminiProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiAiClient(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    @Override
    public AiTriageOutput triageTicket(AiTriageRequest request) {
        if (geminiProperties.getApiKey() == null || geminiProperties.getApiKey().isBlank()) {
            throw new InvalidOperationException("Gemini API key is not configured");
        }

        String prompt = buildPrompt(request);

        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", prompt)
                                }
                        )
                },
                "generationConfig", Map.of(
                        "responseMimeType", "application/json"
                )
        );

        String response;
        try {
            response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + geminiProperties.getModel() + ":generateContent")
                            .queryParam("key", geminiProperties.getApiKey())
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(Math.max(1, geminiProperties.getTimeoutSeconds())));
        } catch (RuntimeException exception) {
            throw new InvalidOperationException("Gemini request failed");
        }

        return parseGeminiResponse(response);
    }

    private String buildPrompt(AiTriageRequest request) {
        return """
                You are assisting an operations team with ticket triage.

                Classify the following operational ticket.

                Rules:
                - Return only valid JSON.
                - Do not include markdown.
                - Do not include ```json fences.
                - Do not invent facts not present in the ticket.
                - If uncertain, lower the confidence score.
                - Always require human review for HIGH or CRITICAL severity.
                - Use one of the allowed categories.
                - Use one of the allowed severities.

                Allowed categories:
                ACCESS, PERFORMANCE, CONFIGURATION, BUG, INFRASTRUCTURE, USER_SUPPORT, SECURITY, DATA_ISSUE, OTHER

                Allowed severities:
                LOW, MEDIUM, HIGH, CRITICAL

                Ticket:
                Title: %s
                Description: %s
                Source System: %s
                Affected Service: %s

                Return exactly this JSON shape:
                {
                  "summary": "",
                  "category": "",
                  "severity": "",
                  "probableCause": "",
                  "recommendedAction": "",
                  "confidenceScore": 0.0,
                  "requiresHumanReview": true
                }
                """.formatted(
                request.title(),
                request.description(),
                request.sourceSystem(),
                request.affectedService()
        );
    }

    AiTriageOutput parseGeminiResponse(String rawResponse) {
        try {
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new InvalidOperationException("Gemini returned an empty response");
            }

            JsonNode root = objectMapper.readTree(rawResponse);

            String text = root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asString();

            if (text == null || text.isBlank()) {
                throw new InvalidOperationException("Gemini returned an empty triage payload");
            }

            String cleanedJson = cleanJson(text);
            JsonNode triageJson = objectMapper.readTree(cleanedJson);

            String summary = requiredText(triageJson, "summary");
            TicketCategory category = TicketCategory.valueOf(normalizeEnumValue(requiredText(triageJson, "category")));
            TicketSeverity severity = TicketSeverity.valueOf(normalizeEnumValue(requiredText(triageJson, "severity")));
            String probableCause = optionalText(
                    triageJson,
                    "probableCause",
                    "Probable cause could not be determined from the ticket details."
            );
            String recommendedAction = optionalText(
                    triageJson,
                    "recommendedAction",
                    "Review the ticket details and assign the appropriate operations owner."
            );
            double confidenceScore = triageJson.path("confidenceScore").asDouble(0.5);
            boolean requiresHumanReview = triageJson.path("requiresHumanReview").asBoolean(true);

            if (confidenceScore < 0 || confidenceScore > 1) {
                confidenceScore = 0.5;
            }

            if (severity == TicketSeverity.HIGH || severity == TicketSeverity.CRITICAL) {
                requiresHumanReview = true;
            }

            return new AiTriageOutput(
                    summary,
                    category,
                    severity,
                    probableCause,
                    recommendedAction,
                    confidenceScore,
                    requiresHumanReview,
                    geminiProperties.getModel(),
                    rawResponse
            );

        } catch (IllegalArgumentException exception) {
            throw new InvalidOperationException("Gemini returned an unsupported category or severity");
        } catch (InvalidOperationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidOperationException("Failed to parse Gemini triage response");
        }
    }

    private String cleanJson(String text) {
        String cleanedText = text
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int firstBrace = cleanedText.indexOf('{');
        int lastBrace = cleanedText.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return cleanedText.substring(firstBrace, lastBrace + 1);
        }

        return cleanedText;
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asString();

        if (value == null || value.isBlank()) {
            throw new InvalidOperationException("Gemini response missing field: " + fieldName);
        }

        return value;
    }

    private String optionalText(JsonNode node, String fieldName, String fallback) {
        String value = node.path(fieldName).asString();

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }

    private String normalizeEnumValue(String value) {
        return value
                .trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }
}
