package com.anushabazaar.backend.domain;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum Role {
        SUPER_ADMIN,
        ADMIN,
        FINANCE,
        KYC_MANAGER,
        OPERATIONS,
        SUPPORT,
        AUDITOR,
        INVESTOR
    }

    public enum KycStatus {
        NOT_SUBMITTED,
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum AccountStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }

    public enum InvestmentStatus {
        PENDING_RECEIPT,
        RECEIPT_UPLOADED,
        ACTIVE,
        PAUSED,
        CLOSED,
        MATURED,
        CANCELLED,
        EARLY_WITHDRAWAL,
        REJECTED
    }

    public enum ReceiptStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum PaymentMode {
        NEFT,
        RTGS,
        IMPS,
        UPI,
        CASH
    }

    public enum WalletTransactionType {
        INTEREST_CREDIT,
        REFERRAL_COMMISSION,
        WITHDRAWAL_DEBIT,
        INVESTMENT_CREDIT,
        INVESTMENT_DEBIT,
        REFUND,
        REFUND_CREDIT,
        PENALTY,
        ADMIN_ADJUSTMENT
    }

    public enum TransactionType {
        INTEREST_CREDIT,
        REFERRAL_COMMISSION,
        WITHDRAWAL_DEBIT,
        INVESTMENT_CREDIT,
        INVESTMENT_DEBIT,
        REFUND,
        REFUND_CREDIT,
        PENALTY,
        ADMIN_ADJUSTMENT
    }

    public enum Direction {
        CREDIT,
        DEBIT
    }

    public enum WithdrawalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        PROCESSED
    }

    public enum NotificationType {
        KYC_UPDATE,
        INVESTMENT_UPDATE,
        INTEREST_CREDITED,
        WITHDRAWAL_UPDATE,
        REFERRAL_COMMISSION,
        FRAUD_ALERT,
        SYSTEM
    }

    public enum NotificationChannel {
        EMAIL,
        IN_APP,
        BOTH
    }

    public enum ActorRole {
        ADMIN,
        INVESTOR,
        SYSTEM
    }

    public enum AlertLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum AlertStatus {
        OPEN,
        UNDER_REVIEW,
        RESOLVED,
        FALSE_POSITIVE
    }

    public enum CommissionStatus {
        CALCULATED,
        CREDITED,
        SKIPPED
    }

    public enum InterestStatus {
        CALCULATED,
        CREDITED,
        SKIPPED
    }

    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET,
        REFRESH
    }

    public enum WhatsappStatus {
        NOT_SENT,
        QUEUED,
        SENDING,
        SENT,
        DELIVERED,
        READ,
        FAILED
    }

    public enum EmailStatus {
        NOT_SENT,
        QUEUED,
        SENDING,
        SENT,
        DELIVERED,
        FAILED
    }

    public enum CouponType {
        FLAT_CASHBACK,
        PERCENTAGE_DISCOUNT,
        BONUS_INTEREST
    }

    public enum CouponStatus {
        ACTIVE,
        INACTIVE,
        EXPIRED
    }

    public enum CouponRedemptionStatus {
        APPLIED,
        REDEEMED,
        CANCELLED
    }

    public enum SupportTicketPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    public enum SupportTicketStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    public enum PlanStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        ACTIVE,
        PAUSED,
        CLOSED
    }
}
