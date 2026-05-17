package com.opspilot.triage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiTriageRepository extends JpaRepository<AiTriageResult, Long> {
    List<AiTriageResult> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}