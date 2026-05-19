package com.opspilot.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalRepository extends JpaRepository<ApprovalDecision, Long> {
    List<ApprovalDecision> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}