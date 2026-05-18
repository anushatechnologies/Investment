package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

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
