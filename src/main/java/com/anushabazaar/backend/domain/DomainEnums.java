package com.anushabazaar.backend.domain;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum Role {
        SUPER_ADMIN,
        ADMIN,
        INVESTOR
    }

    public enum KycStatus {
        NOT_SUBMITTED,
        PENDING,
        APPROVED,
        REJECTED,
        REUPLOAD_REQUIRED
    }

    public enum DocumentReviewStatus {
        NOT_UPLOADED,
        PENDING,
        APPROVED,
        REJECTED,
        REUPLOAD_REQUIRED
    }

    public enum AccountStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }

    public enum OnboardingStatus {
        MOBILE_VERIFIED,
        PROFILE_COMPLETED,
        PASSWORD_CREATED,
        TERMS_ACCEPTED,
        KYC_COMPLETED,
        BANK_LINKED,
        ACCOUNT_ACTIVATED,
        MPIN_CREATED,
        OTP_VERIFIED,
        REGISTERED,
        KYC_PENDING,
        BANK_PENDING,
        ACTIVE
    }

    public enum InvestmentStatus {
        PENDING_RECEIPT,
        RECEIPT_UPLOADED,
        ACTIVE,
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
        CASH,
        CARD,
        NETBANKING,
        WALLET
    }

    public enum WalletTransactionType {
        INTEREST_CREDIT,
        REFERRAL_COMMISSION,
        WITHDRAWAL_DEBIT,
        INVESTMENT_CREDIT,
        COUPON_CASHBACK,
        REFUND,
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
        SIGNUP_EMAIL_OTP,
        SIGNUP_MOBILE_OTP,
        SIGNUP_VERIFICATION,
        PASSWORD_RESET,
        REFRESH
    }

    public enum SupportTicketStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    public enum SupportTicketPriority {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum CouponType {
        FLAT_CASHBACK,
        PERCENT_CASHBACK
    }

    public enum CouponStatus {
        ACTIVE,
        INACTIVE,
        EXPIRED
    }

    public enum CouponRedemptionStatus {
        RESERVED,
        CREDITED,
        CANCELLED
    }
}
