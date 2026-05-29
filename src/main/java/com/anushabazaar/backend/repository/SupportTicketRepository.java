package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(String userId);
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(DomainEnums.SupportTicketStatus status);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
}
