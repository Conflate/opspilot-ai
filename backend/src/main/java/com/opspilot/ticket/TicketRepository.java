package com.opspilot.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByStatusIn(List<TicketStatus> statuses);

    long countByStatus(TicketStatus status);

    long countBySeverity(TicketSeverity severity);
}