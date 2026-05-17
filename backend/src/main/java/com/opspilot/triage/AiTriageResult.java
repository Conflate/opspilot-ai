package com.opspilot.triage;

import com.opspilot.ticket.TicketCategory;
import com.opspilot.ticket.TicketSeverity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ai_triage_results")
public class AiTriageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    private TicketSeverity severity;

    @Column(columnDefinition = "TEXT")
    private String probableCause;

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    private Double confidenceScore;

    private Boolean requiresHumanReview;

    private String modelName;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    private Instant createdAt;

    public AiTriageResult() {
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public TicketSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(TicketSeverity severity) {
        this.severity = severity;
    }

    public String getProbableCause() {
        return probableCause;
    }

    public void setProbableCause(String probableCause) {
        this.probableCause = probableCause;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Boolean getRequiresHumanReview() {
        return requiresHumanReview;
    }

    public void setRequiresHumanReview(Boolean requiresHumanReview) {
        this.requiresHumanReview = requiresHumanReview;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}