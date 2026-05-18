package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findByEntityTypeContainingIgnoreCaseOrActionContainingIgnoreCaseOrderByOccurredAtDesc(String entityType, String action);
    List<AuditLog> findAllByOrderByOccurredAtDesc();
}
