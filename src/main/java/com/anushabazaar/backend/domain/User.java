package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String passwordHash;
    private LocalDate dateOfBirth;
    private String panNumber;
    private String aadhaarLast4;
    private String address;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String referralCode;
    private String referredByCode;
    @Enumerated(EnumType.STRING)
    private DomainEnums.KycStatus kycStatus;
    @Enumerated(EnumType.STRING)
    private DomainEnums.AccountStatus accountStatus;
    @Enumerated(EnumType.STRING)
    private DomainEnums.Role role;
    private boolean riskDisclosureAccepted;
    private LocalDateTime riskDisclosureDate;
    private boolean investorAgreementAccepted;
    private LocalDateTime investorAgreementDate;
    private boolean emailVerified;
    private int failedLoginAttempts;
    private LocalDateTime accountLockedUntil;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private boolean bankVerified;
    private LocalDateTime bankVerifiedAt;
    private String mpinHash;
    private boolean biometricEnabled;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public String getAadhaarLast4() { return aadhaarLast4; }
    public void setAadhaarLast4(String aadhaarLast4) { this.aadhaarLast4 = aadhaarLast4; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfscCode() { return bankIfscCode; }
    public void setBankIfscCode(String bankIfscCode) { this.bankIfscCode = bankIfscCode; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public String getReferredByCode() { return referredByCode; }
    public void setReferredByCode(String referredByCode) { this.referredByCode = referredByCode; }
    public DomainEnums.KycStatus getKycStatus() { return kycStatus; }
    public void setKycStatus(DomainEnums.KycStatus kycStatus) { this.kycStatus = kycStatus; }
    public DomainEnums.AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(DomainEnums.AccountStatus accountStatus) { this.accountStatus = accountStatus; }
    public DomainEnums.Role getRole() { return role; }
    public void setRole(DomainEnums.Role role) { this.role = role; }
    public boolean isRiskDisclosureAccepted() { return riskDisclosureAccepted; }
    public void setRiskDisclosureAccepted(boolean riskDisclosureAccepted) { this.riskDisclosureAccepted = riskDisclosureAccepted; }
    public LocalDateTime getRiskDisclosureDate() { return riskDisclosureDate; }
    public void setRiskDisclosureDate(LocalDateTime riskDisclosureDate) { this.riskDisclosureDate = riskDisclosureDate; }
    public boolean isInvestorAgreementAccepted() { return investorAgreementAccepted; }
    public void setInvestorAgreementAccepted(boolean investorAgreementAccepted) { this.investorAgreementAccepted = investorAgreementAccepted; }
    public LocalDateTime getInvestorAgreementDate() { return investorAgreementDate; }
    public void setInvestorAgreementDate(LocalDateTime investorAgreementDate) { this.investorAgreementDate = investorAgreementDate; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public boolean isBankVerified() { return bankVerified; }
    public void setBankVerified(boolean bankVerified) { this.bankVerified = bankVerified; }
    public LocalDateTime getBankVerifiedAt() { return bankVerifiedAt; }
    public void setBankVerifiedAt(LocalDateTime bankVerifiedAt) { this.bankVerifiedAt = bankVerifiedAt; }
    public String getMpinHash() { return mpinHash; }
    public void setMpinHash(String mpinHash) { this.mpinHash = mpinHash; }
    public boolean isBiometricEnabled() { return biometricEnabled; }
    public void setBiometricEnabled(boolean biometricEnabled) { this.biometricEnabled = biometricEnabled; }
}
