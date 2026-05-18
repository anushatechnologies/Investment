package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class ReferralCommission {

    @Id
    private String id;
    private String beneficiaryUserId;
    private String sourceInvestorId;
    private String sourceInvestmentId;
    private String commissionMonth;
    private Integer referralLevel;
    private BigDecimal commissionRate;
    private BigDecimal sourceInterestAmount;
    private BigDecimal commissionAmount;
    @Enumerated(EnumType.STRING)
    private DomainEnums.CommissionStatus status;
    private String skipReason;
    private LocalDateTime creditedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBeneficiaryUserId() { return beneficiaryUserId; }
    public void setBeneficiaryUserId(String beneficiaryUserId) { this.beneficiaryUserId = beneficiaryUserId; }
    public String getSourceInvestorId() { return sourceInvestorId; }
    public void setSourceInvestorId(String sourceInvestorId) { this.sourceInvestorId = sourceInvestorId; }
    public String getSourceInvestmentId() { return sourceInvestmentId; }
    public void setSourceInvestmentId(String sourceInvestmentId) { this.sourceInvestmentId = sourceInvestmentId; }
    public String getCommissionMonth() { return commissionMonth; }
    public void setCommissionMonth(String commissionMonth) { this.commissionMonth = commissionMonth; }
    public Integer getReferralLevel() { return referralLevel; }
    public void setReferralLevel(Integer referralLevel) { this.referralLevel = referralLevel; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getSourceInterestAmount() { return sourceInterestAmount; }
    public void setSourceInterestAmount(BigDecimal sourceInterestAmount) { this.sourceInterestAmount = sourceInterestAmount; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public DomainEnums.CommissionStatus getStatus() { return status; }
    public void setStatus(DomainEnums.CommissionStatus status) { this.status = status; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public LocalDateTime getCreditedAt() { return creditedAt; }
    public void setCreditedAt(LocalDateTime creditedAt) { this.creditedAt = creditedAt; }
}
