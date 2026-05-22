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
            @NotBlank @Email String email,
            @Pattern(regexp = "\\d{10}") String mobileNumber,
            @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$") String password,
            @NotNull LocalDate dateOfBirth,
            @NotBlank String panNumber,
            @Pattern(regexp = "\\d{4}") String aadhaarLast4,
            @NotBlank String address,
            @NotBlank String bankAccountNumber,
            @NotBlank String bankIfscCode,
            @NotBlank String bankName,
            String referredByCode,
            String signupVerificationToken,
            boolean riskDisclosureAccepted,
            boolean investorAgreementAccepted
    ) {
    }

    public record LoginRequest(@Email String email,
                               @Pattern(regexp = "\\d{10}") String mobileNumber,
                               String password,
                               @Pattern(regexp = "\\d{4,6}") String mpin) {
    }

    public record VerifyMpinRequest(@Pattern(regexp = "\\d{4,6}") String mpin) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record ForgotPasswordRequest(@Email String email, @Pattern(regexp = "\\d{10}") String mobileNumber) {
    }

    public record ResetPasswordRequest(@NotBlank String token,
                                       @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$") String newPassword) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$") String newPassword) {
    }

    public record SendOtpRequest(@Pattern(regexp = "^(\\+91)?\\d{10}$") String mobileNumber,
                                 @Email String email,
                                 String countryCode,
                                 String channel,
                                 Boolean useFirebase,
                                 String type) {
    }

    public record VerifyOtpRequest(String idToken,
                                   @Pattern(regexp = "^(\\+91)?\\d{10}$") String mobileNumber,
                                   @Email String email,
                                   @Pattern(regexp = "\\d{6}") String otp,
                                   String channel,
                                   String type) {
    }

    public record FirebaseMobileLoginRequest(@NotBlank String idToken) {
    }

    public record MobileOnboardingRegisterRequest(
            @NotBlank String idToken,
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$") String password,
            String referredByCode,
            String signupVerificationToken,
            boolean termsAccepted,
            boolean privacyPolicyAccepted,
            boolean kycConsentAccepted
    ) {
    }

    public record SetMpinRequest(@Pattern(regexp = "\\d{4,6}") String mpin) {
    }

    public record EnableBiometricRequest(@NotBlank String deviceId, boolean enabled) {
    }

    public record VerifyBankRequest(@NotBlank String accountHolderName,
                                    @NotBlank String bankAccountNumber,
                                    @NotBlank String confirmBankAccountNumber,
                                    @NotBlank String bankIfscCode,
                                    String bankName) {
    }

    public record FirebaseMobileRegisterRequest(
            @NotBlank String idToken,
            @NotBlank String fullName,
            @NotBlank @Email String email,
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

    public record KycDecisionRequest(String reason, String adminNotes) {
    }

    public record KycDocumentRejectionRequest(String reason,
                                             String adminNotes,
                                             boolean panCard,
                                             boolean aadhaarFront,
                                             boolean aadhaarBack,
                                             boolean selfie,
                                             boolean bankProof) {
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
}
