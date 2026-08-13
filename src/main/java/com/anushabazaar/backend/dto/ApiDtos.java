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
            String fullName,
            String name,
            String email,
            String mobileNumber,
            String phone,
            String password,
            String mpin,
            Object dateOfBirth,
            String dob,
            String panNumber,
            String pan,
            String aadhaarLast4,
            String aadhaar,
            String address,
            String bankAccountNumber,
            String accountNumber,
            String bankIfscCode,
            String ifsc,
            String bankName,
            String referredByCode,
            String referralCode,
            Boolean riskDisclosureAccepted,
            Boolean investorAgreementAccepted,
            Boolean termsAccepted,
            Boolean promotionalConsent
    ) {
    }

    public record LoginRequest(
            String email,
            String mobileNumber,
            String username,
            String phone,
            String password,
            String mpin
    ) {
    }

    public record MobileLoginRequest(
            String mobileNumber,
            String phone,
            String mpin,
            String password
    ) {
    }

    public record AdminLoginRequest(
            String email,
            String username,
            String password,
            String twoFactorCode
    ) {
    }

    public record Verify2faRequest(
            String tempToken,
            String code
    ) {
    }

    public record CreateAdminStaffRequest(
            String fullName,
            String email,
            String mobileNumber,
            String password,
            DomainEnums.Role role
    ) {
    }

    public record SendOtpRequest(
            String email,
            String mobileNumber,
            String phone,
            String type,
            String channel
    ) {
    }

    public record VerifyOtpRequest(
            String email,
            String mobileNumber,
            String phone,
            String otp,
            String code,
            String type
    ) {
    }

    public record SetMpinRequest(
            String mpin,
            String password
    ) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@Email String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token, @Size(min = 8) String newPassword) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword, @Size(min = 8) String newPassword) {
    }

    public record ForgotMpinRequest(String mobileNumber, String phone) {
    }

    public record VerifyResetMpinOtpRequest(String mobileNumber, String phone, String otp, String code) {
    }

    public record ResetMpinRequest(String mobileNumber, String phone, String resetToken, String token, String newMpin, String mpin) {
    }

    public record ChangeMpinRequest(String currentMpin, String newMpin) {
    }

    public record VerifyPanRequest(String panNumber, String pan) {
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
