package com.opspilot.approval;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "approval_decisions")
public class ApprovalDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    private Long triageResultId;

    @Enumerated(EnumType.STRING)
    private ApprovalDecisionType decision;

    private String reviewer;

    @Column(columnDefinition = "TEXT")
    private String reviewNote;

    private Instant createdAt;

    public ApprovalDecision() {
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

    public Long getTriageResultId() {
        return triageResultId;
    }

    public void setTriageResultId(Long triageResultId) {
        this.triageResultId = triageResultId;
    }

    public ApprovalDecisionType getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecisionType decision) {
        this.decision = decision;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public void setReviewNote(String reviewNote) {
        this.reviewNote = reviewNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}