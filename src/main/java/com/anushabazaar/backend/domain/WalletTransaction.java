package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
public class WalletTransaction {

    @Id
    private String id;
    private String walletId;
    private String userId;
    @Enumerated(EnumType.STRING)
    private DomainEnums.WalletTransactionType transactionType;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private DomainEnums.Direction direction;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
    private String createdBy;
    @Transient
    private Map<String, Object> receipt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public DomainEnums.WalletTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(DomainEnums.WalletTransactionType transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public DomainEnums.Direction getDirection() { return direction; }
    public void setDirection(DomainEnums.Direction direction) { this.direction = direction; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Map<String, Object> getReceipt() { return receipt; }
    public void setReceipt(Map<String, Object> receipt) { this.receipt = receipt; }
}
