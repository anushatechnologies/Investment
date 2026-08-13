package com.anushabazaar.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
public class SupportTicket {

    @Id
    private String id;
    private String userId;
    private String category;
    private String subject;
    @Lob
    private String message;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.SupportTicketPriority priority;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.SupportTicketStatus status;
    @Lob
    private String adminReply;
    private String respondedByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public DomainEnums.SupportTicketPriority getPriority() { return priority; }
    public void setPriority(DomainEnums.SupportTicketPriority priority) { this.priority = priority; }
    public DomainEnums.SupportTicketStatus getStatus() { return status; }
    public void setStatus(DomainEnums.SupportTicketStatus status) { this.status = status; }
    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }
    public String getRespondedByAdminId() { return respondedByAdminId; }
    public void setRespondedByAdminId(String respondedByAdminId) { this.respondedByAdminId = respondedByAdminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setAssignedAdminId(String adminId) { this.respondedByAdminId = adminId; }
    public void setAdminResponse(String reply) { this.adminReply = reply; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.updatedAt = respondedAt; }
}
