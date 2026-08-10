package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class WithdrawalRequest {

    @Id
    private String id;
    private String investorId;
    private BigDecimal requestedAmount;
    private BigDecimal walletBalanceAtRequest;
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankName;
    private String accountHolderName;
    @Enumerated(EnumType.STRING)
    private DomainEnums.WithdrawalStatus status;
    private LocalDateTime requestedAt;
    private String reviewedByAdminId;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private String bankTransferReference;
    private String adminNotes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
    public BigDecimal getWalletBalanceAtRequest() { return walletBalanceAtRequest; }
    public void setWalletBalanceAtRequest(BigDecimal walletBalanceAtRequest) { this.walletBalanceAtRequest = walletBalanceAtRequest; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public DomainEnums.WithdrawalStatus getStatus() { return status; }
    public void setStatus(DomainEnums.WithdrawalStatus status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public String getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(String reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getBankTransferReference() { return bankTransferReference; }
    public void setBankTransferReference(String bankTransferReference) { this.bankTransferReference = bankTransferReference; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
