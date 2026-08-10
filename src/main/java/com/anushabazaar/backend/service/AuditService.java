package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.AuditLog;
import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User actor, String action, String entityType, String entityId, String oldValue, String newValue, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID().toString());
        log.setActorUserId(actor.getId());
        log.setActorRole(actor.getRole() == DomainEnums.Role.INVESTOR ? DomainEnums.ActorRole.INVESTOR : DomainEnums.ActorRole.ADMIN);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setOccurredAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    public void log(User actor, String action, String status, String details, HttpServletRequest request) {
        log(actor, action, "SYSTEM", action, status, details, request);
    }

    public void logSystem(String action, String entityType, String entityId, String newValue) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID().toString());
        log.setActorUserId("SYSTEM");
        log.setActorRole(DomainEnums.ActorRole.SYSTEM);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setNewValue(newValue);
        log.setIpAddress("127.0.0.1");
        log.setUserAgent("SYSTEM");
        log.setOccurredAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
