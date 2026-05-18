package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class InterestRecord {

    @Id
    private String id;
    private String investmentId;
    private String investorId;
    private String calculationMonth;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private BigDecimal interestAmount;
    @Enumerated(EnumType.STRING)
    private DomainEnums.InterestStatus status;
    private String skipReason;
    private LocalDateTime calculatedAt;
    private LocalDateTime creditedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }
    public String getCalculationMonth() { return calculationMonth; }
    public void setCalculationMonth(String calculationMonth) { this.calculationMonth = calculationMonth; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }
    public DomainEnums.InterestStatus getStatus() { return status; }
    public void setStatus(DomainEnums.InterestStatus status) { this.status = status; }
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    public LocalDateTime getCreditedAt() { return creditedAt; }
    public void setCreditedAt(LocalDateTime creditedAt) { this.creditedAt = creditedAt; }
}
