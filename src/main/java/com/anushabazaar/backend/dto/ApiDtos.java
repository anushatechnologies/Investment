package com.anushabazaar.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email String email,
            @Pattern(regexp = "\\d{10}") String mobileNumber,
            @Size(min = 8) String password,
            @NotNull LocalDate dateOfBirth,
            @NotBlank String panNumber,
            @Pattern(regexp = "\\d{4}") String aadhaarLast4,
            @NotBlank String address,
            @NotBlank String bankAccountNumber,
            @NotBlank String bankIfscCode,
            @NotBlank String bankName,
            String referredByCode,
            boolean riskDisclosureAccepted,
            boolean investorAgreementAccepted
    ) {
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@Email String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token, @Size(min = 8) String newPassword) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @Size(min = 8) String newPassword) {
    }

    public record KycDecisionRequest(String reason, String adminNotes) {
    }

    public record CreatePlanRequest(
            @NotBlank String planName,
            @NotBlank String description,
            @NotNull @DecimalMin("5000") BigDecimal minimumAmount,
            @NotNull BigDecimal maximumAmount,
            @NotNull Integer lockInMonths,
            @NotNull BigDecimal monthlyInterestRate
    ) {
    }

    public record UpdatePlanRequest(
            @NotBlank String planName,
            @NotBlank String description,
            @NotNull @DecimalMin("5000") BigDecimal minimumAmount,
            @NotNull BigDecimal maximumAmount,
            @NotNull Integer lockInMonths,
            @NotNull BigDecimal monthlyInterestRate,
            boolean active
    ) {
    }

    public record ApplyInvestmentRequest(@NotBlank String investmentPlanId, @NotNull BigDecimal investmentAmount) {
    }

    public record VerifyReceiptRequest(boolean approved, String rejectionReason) {
    }

    public record ActivateInvestmentRequest(String notes) {
    }

    public record CancelInvestmentRequest(String reason) {
    }

    public record RequestWithdrawalRequest(@NotNull BigDecimal requestedAmount) {
    }

    public record WithdrawalDecisionRequest(String reason, String adminNotes) {
    }

    public record WithdrawalProcessRequest(@NotBlank String bankTransferReference, String adminNotes) {
    }

    public record UpdateRateRequest(@NotNull BigDecimal monthlyInterestRate) {
    }

    public record ResolveAlertRequest(String resolutionNotes, String status) {
    }

    public record SuspendUserRequest(String reason) {
    }

    public record UpdateNotificationPreferencesRequest(boolean email, boolean whatsapp, boolean sms, boolean push) {
    }

    public record VerifyRazorpayPaymentRequest(
            String razorpayPaymentId,
            String razorpayOrderId,
            String razorpaySignature,
            String investmentId
    ) {
    }

    public record ValidateCouponRequest(String code, BigDecimal amount) {
    }

    public record CreateCouponRequest(String code, String title, String type, BigDecimal valueAmount, BigDecimal minimumInvestmentAmount) {
    }

    public record UpdateCouponRequest(String title, String type, BigDecimal valueAmount, String status) {
    }

    public record UpdateLegalDocumentRequest(String title, String summary, String content, String effectiveDate) {
    }

    public record ReceiptStatusDetails(
            String receiptNumber,
            String receiptUrl,
            String emailStatus,
            String whatsappStatus,
            boolean available
    ) {
    }

    public record VerifyBankRequest(
            String accountHolderName,
            String bankAccountNumber,
            String confirmBankAccountNumber,
            String bankIfscCode,
            String bankName
    ) {
    }

    public record ReceiptStatusResponse(
            String paymentStatus,
            String investmentId,
            BigDecimal amount,
            ReceiptStatusDetails receipt
    ) {
    }
}
