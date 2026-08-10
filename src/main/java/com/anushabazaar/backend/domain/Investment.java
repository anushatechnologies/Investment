package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Investment {

    @Id
    private String id;
    private String investorUserId;
    private String investmentPlanId;
    private BigDecimal investmentAmount;
    @Enumerated(EnumType.STRING)
    private DomainEnums.InvestmentStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime activatedAt;
    private LocalDate maturityDate;
    private BigDecimal monthlyInterestRate;
    private BigDecimal totalInterestEarned;
    private BigDecimal totalPrincipalReturned;
    private LocalDateTime earlyWithdrawalDate;
    private BigDecimal earlyWithdrawalPenalty;
    private String cancellationReason;
    private String activatedByAdminId;
    private String notes;
    private boolean receiptApproved;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInvestorUserId() { return investorUserId; }
    public void setInvestorUserId(String investorUserId) { this.investorUserId = investorUserId; }
    public String getInvestmentPlanId() { return investmentPlanId; }
    public void setInvestmentPlanId(String investmentPlanId) { this.investmentPlanId = investmentPlanId; }
    public BigDecimal getInvestmentAmount() { return investmentAmount; }
    public void setInvestmentAmount(BigDecimal investmentAmount) { this.investmentAmount = investmentAmount; }
    public DomainEnums.InvestmentStatus getStatus() { return status; }
    public void setStatus(DomainEnums.InvestmentStatus status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public BigDecimal getMonthlyInterestRate() { return monthlyInterestRate; }
    public void setMonthlyInterestRate(BigDecimal monthlyInterestRate) { this.monthlyInterestRate = monthlyInterestRate; }
    public BigDecimal getTotalInterestEarned() { return totalInterestEarned; }
    public void setTotalInterestEarned(BigDecimal totalInterestEarned) { this.totalInterestEarned = totalInterestEarned; }
    public BigDecimal getTotalPrincipalReturned() { return totalPrincipalReturned; }
    public void setTotalPrincipalReturned(BigDecimal totalPrincipalReturned) { this.totalPrincipalReturned = totalPrincipalReturned; }
    public LocalDateTime getEarlyWithdrawalDate() { return earlyWithdrawalDate; }
    public void setEarlyWithdrawalDate(LocalDateTime earlyWithdrawalDate) { this.earlyWithdrawalDate = earlyWithdrawalDate; }
    public BigDecimal getEarlyWithdrawalPenalty() { return earlyWithdrawalPenalty; }
    public void setEarlyWithdrawalPenalty(BigDecimal earlyWithdrawalPenalty) { this.earlyWithdrawalPenalty = earlyWithdrawalPenalty; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public String getActivatedByAdminId() { return activatedByAdminId; }
    public void setActivatedByAdminId(String activatedByAdminId) { this.activatedByAdminId = activatedByAdminId; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isReceiptApproved() { return receiptApproved; }
    public void setReceiptApproved(boolean receiptApproved) { this.receiptApproved = receiptApproved; }
}
