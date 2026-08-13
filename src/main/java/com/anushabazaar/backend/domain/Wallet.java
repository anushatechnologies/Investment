package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Wallet {

    @Id
    private String id;
    private String userId;
    private BigDecimal availableBalance;
    private BigDecimal totalCredited;
    private BigDecimal totalDebited;
    private BigDecimal lockedBalance;
    private LocalDateTime lastUpdatedAt;
    private Long versionValue;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public BigDecimal getBalance() { return availableBalance != null ? availableBalance : BigDecimal.ZERO; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public BigDecimal getTotalCredited() { return totalCredited; }
    public void setTotalCredited(BigDecimal totalCredited) { this.totalCredited = totalCredited; }
    public BigDecimal getTotalDebited() { return totalDebited; }
    public void setTotalDebited(BigDecimal totalDebited) { this.totalDebited = totalDebited; }
    public BigDecimal getLockedBalance() { return lockedBalance; }
    public void setLockedBalance(BigDecimal lockedBalance) { this.lockedBalance = lockedBalance; }
    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }
    public Long getVersionValue() { return versionValue; }
    public void setVersionValue(Long versionValue) { this.versionValue = versionValue; }
}
