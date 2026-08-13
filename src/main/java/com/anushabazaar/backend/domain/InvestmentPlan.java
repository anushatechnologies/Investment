package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class InvestmentPlan {

    @Id
    private String id;
    private String planName;
    private String description;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Integer lockInMonths;
    private BigDecimal monthlyInterestRate;
    private boolean active;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private DomainEnums.PlanStatus planStatus = DomainEnums.PlanStatus.ACTIVE;
    private String planCode;
    private String category;
    private String returnType = "FIXED";
    private String payoutFrequency = "MONTHLY";
    private String approvedByAdminId;
    private String approvalNotes;
    private String createdByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    public DomainEnums.PlanStatus getPlanStatus() { return planStatus != null ? planStatus : (active ? DomainEnums.PlanStatus.ACTIVE : DomainEnums.PlanStatus.PAUSED); }
    public void setPlanStatus(DomainEnums.PlanStatus planStatus) { this.planStatus = planStatus; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public String getPayoutFrequency() { return payoutFrequency; }
    public void setPayoutFrequency(String payoutFrequency) { this.payoutFrequency = payoutFrequency; }
    public String getApprovedByAdminId() { return approvedByAdminId; }
    public void setApprovedByAdminId(String approvedByAdminId) { this.approvedByAdminId = approvedByAdminId; }
    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }
    public BigDecimal getMaximumAmount() { return maximumAmount; }
    public void setMaximumAmount(BigDecimal maximumAmount) { this.maximumAmount = maximumAmount; }
    public Integer getLockInMonths() { return lockInMonths; }
    public void setLockInMonths(Integer lockInMonths) { this.lockInMonths = lockInMonths; }
    public BigDecimal getMonthlyInterestRate() { return monthlyInterestRate; }
    public void setMonthlyInterestRate(BigDecimal monthlyInterestRate) { this.monthlyInterestRate = monthlyInterestRate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
}
