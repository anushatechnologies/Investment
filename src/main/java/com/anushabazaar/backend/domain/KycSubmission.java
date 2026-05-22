package com.anushabazaar.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
public class KycSubmission {

    @Id
    private String id;
    private String userId;
    private String panCardPath;
    private String aadhaarFrontPath;
    private String aadhaarBackPath;
    private String selfiePath;
    private String bankProofPath;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.DocumentReviewStatus panCardStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.DocumentReviewStatus aadhaarFrontStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.DocumentReviewStatus aadhaarBackStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.DocumentReviewStatus selfieStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.DocumentReviewStatus bankProofStatus;
    private String panCardRejectionReason;
    private String aadhaarFrontRejectionReason;
    private String aadhaarBackRejectionReason;
    private String selfieRejectionReason;
    private String bankProofRejectionReason;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.KycStatus status;
    private LocalDateTime submittedAt;
    private String reviewedByAdminId;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private String adminNotes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPanCardPath() { return panCardPath; }
    public void setPanCardPath(String panCardPath) { this.panCardPath = panCardPath; }
    public String getAadhaarFrontPath() { return aadhaarFrontPath; }
    public void setAadhaarFrontPath(String aadhaarFrontPath) { this.aadhaarFrontPath = aadhaarFrontPath; }
    public String getAadhaarBackPath() { return aadhaarBackPath; }
    public void setAadhaarBackPath(String aadhaarBackPath) { this.aadhaarBackPath = aadhaarBackPath; }
    public String getSelfiePath() { return selfiePath; }
    public void setSelfiePath(String selfiePath) { this.selfiePath = selfiePath; }
    public String getBankProofPath() { return bankProofPath; }
    public void setBankProofPath(String bankProofPath) { this.bankProofPath = bankProofPath; }
    public DomainEnums.DocumentReviewStatus getPanCardStatus() { return panCardStatus; }
    public void setPanCardStatus(DomainEnums.DocumentReviewStatus panCardStatus) { this.panCardStatus = panCardStatus; }
    public DomainEnums.DocumentReviewStatus getAadhaarFrontStatus() { return aadhaarFrontStatus; }
    public void setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus aadhaarFrontStatus) { this.aadhaarFrontStatus = aadhaarFrontStatus; }
    public DomainEnums.DocumentReviewStatus getAadhaarBackStatus() { return aadhaarBackStatus; }
    public void setAadhaarBackStatus(DomainEnums.DocumentReviewStatus aadhaarBackStatus) { this.aadhaarBackStatus = aadhaarBackStatus; }
    public DomainEnums.DocumentReviewStatus getSelfieStatus() { return selfieStatus; }
    public void setSelfieStatus(DomainEnums.DocumentReviewStatus selfieStatus) { this.selfieStatus = selfieStatus; }
    public DomainEnums.DocumentReviewStatus getBankProofStatus() { return bankProofStatus; }
    public void setBankProofStatus(DomainEnums.DocumentReviewStatus bankProofStatus) { this.bankProofStatus = bankProofStatus; }
    public String getPanCardRejectionReason() { return panCardRejectionReason; }
    public void setPanCardRejectionReason(String panCardRejectionReason) { this.panCardRejectionReason = panCardRejectionReason; }
    public String getAadhaarFrontRejectionReason() { return aadhaarFrontRejectionReason; }
    public void setAadhaarFrontRejectionReason(String aadhaarFrontRejectionReason) { this.aadhaarFrontRejectionReason = aadhaarFrontRejectionReason; }
    public String getAadhaarBackRejectionReason() { return aadhaarBackRejectionReason; }
    public void setAadhaarBackRejectionReason(String aadhaarBackRejectionReason) { this.aadhaarBackRejectionReason = aadhaarBackRejectionReason; }
    public String getSelfieRejectionReason() { return selfieRejectionReason; }
    public void setSelfieRejectionReason(String selfieRejectionReason) { this.selfieRejectionReason = selfieRejectionReason; }
    public String getBankProofRejectionReason() { return bankProofRejectionReason; }
    public void setBankProofRejectionReason(String bankProofRejectionReason) { this.bankProofRejectionReason = bankProofRejectionReason; }
    public DomainEnums.KycStatus getStatus() { return status; }
    public void setStatus(DomainEnums.KycStatus status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(String reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
