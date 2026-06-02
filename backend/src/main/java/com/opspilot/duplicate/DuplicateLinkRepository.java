package com.opspilot.duplicate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DuplicateLinkRepository extends JpaRepository<DuplicateLink, Long> {
    List<DuplicateLink> findBySourceTicketIdOrderByCreatedAtDesc(Long sourceTicketId);

    boolean existsBySourceTicketIdAndDuplicateOfTicketId(Long sourceTicketId, Long duplicateOfTicketId);
}
