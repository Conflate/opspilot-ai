package com.opspilot.duplicate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "duplicate_links")
public class DuplicateLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long sourceTicketId;

    @Column(nullable = false)
    private Long duplicateOfTicketId;

    @Column(nullable = false)
    private String markedBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public DuplicateLink() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSourceTicketId() {
        return sourceTicketId;
    }

    public void setSourceTicketId(Long sourceTicketId) {
        this.sourceTicketId = sourceTicketId;
    }

    public Long getDuplicateOfTicketId() {
        return duplicateOfTicketId;
    }

    public void setDuplicateOfTicketId(Long duplicateOfTicketId) {
        this.duplicateOfTicketId = duplicateOfTicketId;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
