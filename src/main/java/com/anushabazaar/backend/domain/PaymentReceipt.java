package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_receipts")
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

    private String receiptNumber;
    private String receiptUrl;
    @Enumerated(EnumType.STRING)
    private DomainEnums.EmailStatus emailStatus;
    @Enumerated(EnumType.STRING)
    private DomainEnums.WhatsappStatus whatsappStatus;
    private String whatsappPhoneNumber;
    private String whatsappWamid;
    private String whatsappErrorReason;

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

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public DomainEnums.EmailStatus getEmailStatus() { return emailStatus; }
    public void setEmailStatus(DomainEnums.EmailStatus emailStatus) { this.emailStatus = emailStatus; }
    public DomainEnums.WhatsappStatus getWhatsappStatus() { return whatsappStatus; }
    public void setWhatsappStatus(DomainEnums.WhatsappStatus whatsappStatus) { this.whatsappStatus = whatsappStatus; }
    public String getWhatsappPhoneNumber() { return whatsappPhoneNumber; }
    public void setWhatsappPhoneNumber(String whatsappPhoneNumber) { this.whatsappPhoneNumber = whatsappPhoneNumber; }
    public String getWhatsappWamid() { return whatsappWamid; }
    public void setWhatsappWamid(String whatsappWamid) { this.whatsappWamid = whatsappWamid; }
    public String getWhatsappErrorReason() { return whatsappErrorReason; }
    public void setWhatsappErrorReason(String whatsappErrorReason) { this.whatsappErrorReason = whatsappErrorReason; }
}
