package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class PaymentReceipt {

    @Id
    private String id;
    private String investmentId;
    private String investorId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storageKey;
    private LocalDateTime presignedUrlExpiry;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    @Enumerated(EnumType.STRING)
    private DomainEnums.PaymentMode paymentMode;
    private String bankReference;
    @Enumerated(EnumType.STRING)
    private DomainEnums.ReceiptStatus verificationStatus;
    private String verifiedByAdminId;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private LocalDateTime uploadedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public LocalDateTime getPresignedUrlExpiry() { return presignedUrlExpiry; }
    public void setPresignedUrlExpiry(LocalDateTime presignedUrlExpiry) { this.presignedUrlExpiry = presignedUrlExpiry; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public DomainEnums.PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(DomainEnums.PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public String getBankReference() { return bankReference; }
    public void setBankReference(String bankReference) { this.bankReference = bankReference; }
    public DomainEnums.ReceiptStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(DomainEnums.ReceiptStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getVerifiedByAdminId() { return verifiedByAdminId; }
    public void setVerifiedByAdminId(String verifiedByAdminId) { this.verifiedByAdminId = verifiedByAdminId; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
