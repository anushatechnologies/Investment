package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Coupon {

    @Id
    private String id;
    private String code;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private DomainEnums.CouponType type;
    private BigDecimal valueAmount;
    private BigDecimal minimumInvestmentAmount;
    private BigDecimal maximumCashbackAmount;
    private Integer totalUsageLimit;
    private Integer perUserUsageLimit;
    private boolean firstInvestmentOnly;
    @Enumerated(EnumType.STRING)
    private DomainEnums.CouponStatus status;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private String createdByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;
    private String lastModifiedBy;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DomainEnums.CouponType getType() { return type; }
    public void setType(DomainEnums.CouponType type) { this.type = type; }
    public BigDecimal getValueAmount() { return valueAmount; }
    public void setValueAmount(BigDecimal valueAmount) { this.valueAmount = valueAmount; }
    public BigDecimal getMinimumInvestmentAmount() { return minimumInvestmentAmount; }
    public void setMinimumInvestmentAmount(BigDecimal minimumInvestmentAmount) { this.minimumInvestmentAmount = minimumInvestmentAmount; }
    public BigDecimal getMaximumCashbackAmount() { return maximumCashbackAmount; }
    public void setMaximumCashbackAmount(BigDecimal maximumCashbackAmount) { this.maximumCashbackAmount = maximumCashbackAmount; }
    public Integer getTotalUsageLimit() { return totalUsageLimit; }
    public void setTotalUsageLimit(Integer totalUsageLimit) { this.totalUsageLimit = totalUsageLimit; }
    public Integer getPerUserUsageLimit() { return perUserUsageLimit; }
    public void setPerUserUsageLimit(Integer perUserUsageLimit) { this.perUserUsageLimit = perUserUsageLimit; }
    public boolean isFirstInvestmentOnly() { return firstInvestmentOnly; }
    public void setFirstInvestmentOnly(boolean firstInvestmentOnly) { this.firstInvestmentOnly = firstInvestmentOnly; }
    public DomainEnums.CouponStatus getStatus() { return status; }
    public void setStatus(DomainEnums.CouponStatus status) { this.status = status; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
}
