package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class ReferralRelationship {

    @Id
    private String id;
    private String referrerUserId;
    private String referredUserId;
    private Integer referralLevel;
    private boolean active;
    private LocalDateTime linkedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReferrerUserId() { return referrerUserId; }
    public void setReferrerUserId(String referrerUserId) { this.referrerUserId = referrerUserId; }
    public String getReferredUserId() { return referredUserId; }
    public void setReferredUserId(String referredUserId) { this.referredUserId = referredUserId; }
    public Integer getReferralLevel() { return referralLevel; }
    public void setReferralLevel(Integer referralLevel) { this.referralLevel = referralLevel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
}
