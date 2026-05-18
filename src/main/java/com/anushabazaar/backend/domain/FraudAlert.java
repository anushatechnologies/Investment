package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class FraudAlert {

    @Id
    private String id;
    private String userId;
    @Enumerated(EnumType.STRING)
    private DomainEnums.AlertLevel alertLevel;
    private String ruleTriggered;
    private String description;
    @Enumerated(EnumType.STRING)
    private DomainEnums.AlertStatus status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String resolutionNotes;
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public DomainEnums.AlertLevel getAlertLevel() { return alertLevel; }
    public void setAlertLevel(DomainEnums.AlertLevel alertLevel) { this.alertLevel = alertLevel; }
    public String getRuleTriggered() { return ruleTriggered; }
    public void setRuleTriggered(String ruleTriggered) { this.ruleTriggered = ruleTriggered; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DomainEnums.AlertStatus getStatus() { return status; }
    public void setStatus(DomainEnums.AlertStatus status) { this.status = status; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
