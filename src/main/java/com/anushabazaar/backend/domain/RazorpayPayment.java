package com.anushabazaar.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class RazorpayPayment {

    @Id
    private String id;
    private String investmentId;
    private String investorId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String method;
    private Boolean captured;
    private String webhookEventId;
    private String webhookEventType;
    private String settlementId;
    private String settlementStatus;
    private BigDecimal settlementAmount;
    private String settlementUtr;
    private LocalDateTime checkoutOrderCreatedAt;
    private LocalDateTime paymentAuthorizedAt;
    private LocalDateTime paymentCapturedAt;
    private LocalDateTime signatureVerifiedAt;
    private LocalDateTime settlementProcessedAt;
    private LocalDateTime lastSyncedAt;
    @Lob
    private String orderPayload;
    @Lob
    private String paymentPayload;
    @Lob
    private String webhookPayload;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInvestmentId() { return investmentId; }
    public void setInvestmentId(String investmentId) { this.investmentId = investmentId; }
    public String getInvestorId() { return investorId; }
    public void setInvestorId(String investorId) { this.investorId = investorId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Boolean getCaptured() { return captured; }
    public void setCaptured(Boolean captured) { this.captured = captured; }
    public String getWebhookEventId() { return webhookEventId; }
    public void setWebhookEventId(String webhookEventId) { this.webhookEventId = webhookEventId; }
    public String getWebhookEventType() { return webhookEventType; }
    public void setWebhookEventType(String webhookEventType) { this.webhookEventType = webhookEventType; }
    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }
    public String getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }
    public String getSettlementUtr() { return settlementUtr; }
    public void setSettlementUtr(String settlementUtr) { this.settlementUtr = settlementUtr; }
    public LocalDateTime getCheckoutOrderCreatedAt() { return checkoutOrderCreatedAt; }
    public void setCheckoutOrderCreatedAt(LocalDateTime checkoutOrderCreatedAt) { this.checkoutOrderCreatedAt = checkoutOrderCreatedAt; }
    public LocalDateTime getPaymentAuthorizedAt() { return paymentAuthorizedAt; }
    public void setPaymentAuthorizedAt(LocalDateTime paymentAuthorizedAt) { this.paymentAuthorizedAt = paymentAuthorizedAt; }
    public LocalDateTime getPaymentCapturedAt() { return paymentCapturedAt; }
    public void setPaymentCapturedAt(LocalDateTime paymentCapturedAt) { this.paymentCapturedAt = paymentCapturedAt; }
    public LocalDateTime getSignatureVerifiedAt() { return signatureVerifiedAt; }
    public void setSignatureVerifiedAt(LocalDateTime signatureVerifiedAt) { this.signatureVerifiedAt = signatureVerifiedAt; }
    public LocalDateTime getSettlementProcessedAt() { return settlementProcessedAt; }
    public void setSettlementProcessedAt(LocalDateTime settlementProcessedAt) { this.settlementProcessedAt = settlementProcessedAt; }
    public LocalDateTime getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(LocalDateTime lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public String getOrderPayload() { return orderPayload; }
    public void setOrderPayload(String orderPayload) { this.orderPayload = orderPayload; }
    public String getPaymentPayload() { return paymentPayload; }
    public void setPaymentPayload(String paymentPayload) { this.paymentPayload = paymentPayload; }
    public String getWebhookPayload() { return webhookPayload; }
    public void setWebhookPayload(String webhookPayload) { this.webhookPayload = webhookPayload; }
}
