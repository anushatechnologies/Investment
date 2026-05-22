package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private String mpinHash;
    private boolean termsAccepted;
    private LocalDateTime termsAcceptedAt;
    private boolean privacyPolicyAccepted;
    private LocalDateTime privacyPolicyAcceptedAt;
    private boolean kycConsentAccepted;
    private LocalDateTime kycConsentAcceptedAt;
    private boolean biometricEnabled;
    private String biometricDeviceId;
    private LocalDateTime biometricEnabledAt;
    private boolean bankVerified;
    private LocalDateTime bankVerifiedAt;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.OnboardingStatus onboardingStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.KycStatus kycStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
    private DomainEnums.AccountStatus accountStatus;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 50)
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
    public String getMpinHash() { return mpinHash; }
    public void setMpinHash(String mpinHash) { this.mpinHash = mpinHash; }
    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }
    public LocalDateTime getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }
    public boolean isPrivacyPolicyAccepted() { return privacyPolicyAccepted; }
    public void setPrivacyPolicyAccepted(boolean privacyPolicyAccepted) { this.privacyPolicyAccepted = privacyPolicyAccepted; }
    public LocalDateTime getPrivacyPolicyAcceptedAt() { return privacyPolicyAcceptedAt; }
    public void setPrivacyPolicyAcceptedAt(LocalDateTime privacyPolicyAcceptedAt) { this.privacyPolicyAcceptedAt = privacyPolicyAcceptedAt; }
    public boolean isKycConsentAccepted() { return kycConsentAccepted; }
    public void setKycConsentAccepted(boolean kycConsentAccepted) { this.kycConsentAccepted = kycConsentAccepted; }
    public LocalDateTime getKycConsentAcceptedAt() { return kycConsentAcceptedAt; }
    public void setKycConsentAcceptedAt(LocalDateTime kycConsentAcceptedAt) { this.kycConsentAcceptedAt = kycConsentAcceptedAt; }
    public boolean isBiometricEnabled() { return biometricEnabled; }
    public void setBiometricEnabled(boolean biometricEnabled) { this.biometricEnabled = biometricEnabled; }
    public String getBiometricDeviceId() { return biometricDeviceId; }
    public void setBiometricDeviceId(String biometricDeviceId) { this.biometricDeviceId = biometricDeviceId; }
    public LocalDateTime getBiometricEnabledAt() { return biometricEnabledAt; }
    public void setBiometricEnabledAt(LocalDateTime biometricEnabledAt) { this.biometricEnabledAt = biometricEnabledAt; }
    public boolean isBankVerified() { return bankVerified; }
    public void setBankVerified(boolean bankVerified) { this.bankVerified = bankVerified; }
    public LocalDateTime getBankVerifiedAt() { return bankVerifiedAt; }
    public void setBankVerifiedAt(LocalDateTime bankVerifiedAt) { this.bankVerifiedAt = bankVerifiedAt; }
    public DomainEnums.OnboardingStatus getOnboardingStatus() { return onboardingStatus; }
    public void setOnboardingStatus(DomainEnums.OnboardingStatus onboardingStatus) { this.onboardingStatus = onboardingStatus; }
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
}
