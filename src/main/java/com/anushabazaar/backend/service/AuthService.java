package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.TokenRecord;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.domain.Wallet;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.domain.ReferralRelationship;
import com.anushabazaar.backend.repository.ReferralRelationshipRepository;
import com.anushabazaar.backend.repository.TokenRecordRepository;
import com.anushabazaar.backend.repository.UserRepository;
import com.anushabazaar.backend.repository.WalletRepository;
import com.anushabazaar.backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {
    private static final Random OTP_RANDOM = new Random();

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ReferralRelationshipRepository referralRelationshipRepository;
    private final TokenRecordRepository tokenRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final FirebasePhoneAuthService firebasePhoneAuthService;
    private final EmailService emailService;
    private final int refreshExpiryDays;
    private final String frontendBaseUrl;
    private final boolean exposeGeneratedValues;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       ReferralRelationshipRepository referralRelationshipRepository,
                       TokenRecordRepository tokenRecordRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       FirebasePhoneAuthService firebasePhoneAuthService,
                       EmailService emailService,
                       @Value("${app.jwt.refresh-expiry-days}") int refreshExpiryDays,
                       @Value("${app.frontend.base-url}") String frontendBaseUrl,
                       @Value("${app.email.expose-generated-values:true}") boolean exposeGeneratedValues) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.referralRelationshipRepository = referralRelationshipRepository;
        this.tokenRecordRepository = tokenRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.firebasePhoneAuthService = firebasePhoneAuthService;
        this.emailService = emailService;
        this.refreshExpiryDays = refreshExpiryDays;
        this.frontendBaseUrl = frontendBaseUrl;
        this.exposeGeneratedValues = exposeGeneratedValues;
    }

    @Transactional
    public Map<String, Object> register(ApiDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        if (userRepository.findByMobileNumber(request.mobileNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already exists");
        }
        if (!request.riskDisclosureAccepted() || !request.investorAgreementAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mandatory consents must be accepted");
        }
        if (request.dateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investor must be at least 18 years old");
        }
        consumeSignupVerification(request.signupVerificationToken(), request.email(), request.mobileNumber());

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setMobileNumber(request.mobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDateOfBirth(request.dateOfBirth());
        user.setPanNumber(request.panNumber());
        user.setAadhaarLast4(request.aadhaarLast4());
        user.setAddress(request.address());
        user.setBankAccountNumber(request.bankAccountNumber());
        user.setBankIfscCode(request.bankIfscCode());
        user.setBankName(request.bankName());
        user.setReferralCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        user.setReferredByCode(request.referredByCode());
        user.setKycStatus(DomainEnums.KycStatus.NOT_SUBMITTED);
        user.setAccountStatus(DomainEnums.AccountStatus.PENDING);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.REGISTERED);
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setRiskDisclosureAccepted(true);
        user.setRiskDisclosureDate(LocalDateTime.now());
        user.setInvestorAgreementAccepted(true);
        user.setInvestorAgreementDate(LocalDateTime.now());
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setPrivacyPolicyAccepted(true);
        user.setPrivacyPolicyAcceptedAt(LocalDateTime.now());
        user.setKycConsentAccepted(true);
        user.setKycConsentAcceptedAt(LocalDateTime.now());
        user.setBankVerified(false);
        user.setBankVerifiedAt(null);
        user.setEmailVerified(false);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy("SELF");
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID().toString());
        wallet.setUserId(user.getId());
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setTotalCredited(BigDecimal.ZERO);
        wallet.setTotalDebited(BigDecimal.ZERO);
        wallet.setVersionValue(0L);
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        createReferralLinks(user);

        TokenRecord verificationToken = issueToken(user.getId(), DomainEnums.TokenType.EMAIL_VERIFICATION, 2);
        auditService.log(user, "REGISTERED", "User", user.getId(), null, user.getEmail(), servletRequest);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful. Verify email, then complete KYC and bank linking.");
        response.put("verificationToken", verificationToken.getTokenValue());
        response.put("userId", user.getId());
        return response;
    }

    @Transactional
    public Map<String, Object> verifyEmail(String token, HttpServletRequest request) {
        TokenRecord record = tokenRecordRepository.findByTokenValueAndTokenTypeAndUsedFalse(token, DomainEnums.TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }
        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEmailVerified(true);
        user.setAccountStatus(DomainEnums.AccountStatus.PENDING);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.PASSWORD_CREATED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        record.setUsed(true);
        tokenRecordRepository.save(record);
        auditService.log(user, "EMAIL_VERIFIED", "User", user.getId(), null, user.getEmail(), request);
        return Map.of("message", "Email verified successfully");
    }

    @Transactional
    public Map<String, Object> login(ApiDtos.LoginRequest request, HttpServletRequest servletRequest) {
        if (request.email() != null && !request.email().isBlank()) {
            return loginWithPassword(request, servletRequest);
        }
        if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) {
            return loginWithMpin(request, servletRequest);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email/password or mobile/MPIN is required");
    }

    private Map<String, Object> loginWithPassword(ApiDtos.LoginRequest request, HttpServletRequest servletRequest) {
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for email login");
        }
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is temporarily locked");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Verify email before login");
        }
        if (user.getAccountStatus() == DomainEnums.AccountStatus.SUSPENDED || user.getAccountStatus() == DomainEnums.AccountStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(servletRequest.getRemoteAddr());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        TokenRecord refreshToken = issueToken(user.getId(), DomainEnums.TokenType.REFRESH, refreshExpiryDays * 24);
        auditService.log(user, "LOGIN", "User", user.getId(), null, user.getEmail(), servletRequest);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getTokenValue(),
                "role", user.getRole(),
                "userId", user.getId()
        );
    }

    private Map<String, Object> loginWithMpin(ApiDtos.LoginRequest request, HttpServletRequest servletRequest) {
        if (request.mpin() == null || request.mpin().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MPIN is required for mobile login");
        }
        User user = userRepository.findByMobileNumber(request.mobileNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        validateMpin(user, request.mpin());
        ensureMobileLoginAllowed(user);
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(servletRequest.getRemoteAddr());
        userRepository.save(user);
        auditService.log(user, "MPIN_LOGIN", "User", user.getId(), null, user.getMobileNumber(), servletRequest);
        return authResponse(user);
    }

    public Map<String, Object> sendOtp(ApiDtos.SendOtpRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().toLowerCase();
            ensureEmailAvailable(email);
            TokenRecord otp = issueOtp(email, DomainEnums.TokenType.SIGNUP_EMAIL_OTP, 10);
            emailService.sendSignupOtp(email, otp.getTokenValue());
            Map<String, Object> response = new HashMap<>();
            response.put("provider", "EMAIL_OTP");
            response.put("message", emailService.isEnabled() ? "Email OTP sent." : "Email OTP generated. Configure SMTP to send email.");
            response.put("email", email);
            response.put("expiresInMinutes", 10);
            response.put("nextStep", "VERIFY_OTP");
            if (exposeGeneratedValues) {
                response.put("otp", otp.getTokenValue());
            }
            return response;
        }
        String countryCode = request.countryCode() == null || request.countryCode().isBlank() ? "+91" : request.countryCode();
        if (request.mobileNumber() == null || request.mobileNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number or email is required");
        }
        if (Boolean.FALSE.equals(request.useFirebase()) || "MOBILE_OTP".equalsIgnoreCase(request.channel())) {
            ensureMobileAvailable(request.mobileNumber());
            TokenRecord otp = issueOtp(request.mobileNumber(), DomainEnums.TokenType.SIGNUP_MOBILE_OTP, 10);
            Map<String, Object> response = new HashMap<>();
            response.put("provider", "MOBILE_OTP");
            response.put("message", "Mobile OTP generated. Send this OTP using your SMS provider.");
            response.put("phoneNumber", countryCode + request.mobileNumber());
            response.put("mobileNumber", request.mobileNumber());
            response.put("expiresInMinutes", 10);
            response.put("nextStep", "VERIFY_OTP");
            if (exposeGeneratedValues) {
                response.put("otp", otp.getTokenValue());
            }
            return response;
        }
        boolean userExists = userRepository.findByMobileNumber(request.mobileNumber()).isPresent();
        return Map.of(
                "provider", "FIREBASE_PHONE_AUTH",
                "message", "Send OTP from frontend using Firebase Phone Auth.",
                "phoneNumber", countryCode + request.mobileNumber(),
                "mobileNumber", request.mobileNumber(),
                "userExists", userExists,
                "nextStep", "VERIFY_OTP"
        );
    }

    @Transactional
    public Map<String, Object> verifyOtp(ApiDtos.VerifyOtpRequest request, HttpServletRequest servletRequest) {
        if (request.idToken() != null && !request.idToken().isBlank()) {
            return firebaseMobileLogin(new ApiDtos.FirebaseMobileLoginRequest(request.idToken()), servletRequest);
        }
        if (request.email() != null && !request.email().isBlank()) {
            return verifyStoredOtp(request.email().toLowerCase(), request.otp(), DomainEnums.TokenType.SIGNUP_EMAIL_OTP, "EMAIL_VERIFIED");
        }
        if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) {
            return verifyStoredOtp(request.mobileNumber(), request.otp(), DomainEnums.TokenType.SIGNUP_MOBILE_OTP, "MOBILE_VERIFIED");
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase idToken or email/mobile OTP is required");
    }

    @Transactional
    public Map<String, Object> firebaseMobileLogin(ApiDtos.FirebaseMobileLoginRequest request, HttpServletRequest servletRequest) {
        FirebasePhoneAuthService.VerifiedFirebasePhone verifiedPhone = firebasePhoneAuthService.verifyPhoneToken(request.idToken());
        return userRepository.findByMobileNumber(verifiedPhone.mobileNumber())
                .map(user -> {
                    ensureMobileLoginAllowed(user);
                    user.setLastLoginAt(LocalDateTime.now());
                    user.setLastLoginIp(servletRequest.getRemoteAddr());
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                    auditService.log(user, "FIREBASE_MOBILE_LOGIN", "User", user.getId(), null, verifiedPhone.mobileNumber(), servletRequest);
                    Map<String, Object> response = authResponse(user);
                    response.put("userExists", true);
                    response.put("nextStep", nextOnboardingStep(user));
                    return response;
                })
                .orElseGet(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("userExists", false);
                    response.put("nextStep", "REGISTER");
                    response.put("mobileNumber", verifiedPhone.mobileNumber());
                    response.put("firebaseUid", verifiedPhone.firebaseUid());
                    response.put("message", "Mobile verified. Complete registration to create investor account.");
                    return response;
                });
    }

    @Transactional
    public Map<String, Object> firebaseMobileRegister(ApiDtos.FirebaseMobileRegisterRequest request, HttpServletRequest servletRequest) {
        FirebasePhoneAuthService.VerifiedFirebasePhone verifiedPhone = firebasePhoneAuthService.verifyPhoneToken(request.idToken());
        if (userRepository.findByMobileNumber(verifiedPhone.mobileNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already exists");
        }
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        if (!request.riskDisclosureAccepted() || !request.investorAgreementAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mandatory consents must be accepted");
        }
        if (request.dateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investor must be at least 18 years old");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setMobileNumber(verifiedPhone.mobileNumber());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDateOfBirth(request.dateOfBirth());
        user.setPanNumber(request.panNumber());
        user.setAadhaarLast4(request.aadhaarLast4());
        user.setAddress(request.address());
        user.setBankAccountNumber(request.bankAccountNumber());
        user.setBankIfscCode(request.bankIfscCode());
        user.setBankName(request.bankName());
        user.setReferralCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        user.setReferredByCode(request.referredByCode());
        user.setKycStatus(DomainEnums.KycStatus.NOT_SUBMITTED);
        user.setAccountStatus(DomainEnums.AccountStatus.PENDING);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.REGISTERED);
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setRiskDisclosureAccepted(true);
        user.setRiskDisclosureDate(LocalDateTime.now());
        user.setInvestorAgreementAccepted(true);
        user.setInvestorAgreementDate(LocalDateTime.now());
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setPrivacyPolicyAccepted(true);
        user.setPrivacyPolicyAcceptedAt(LocalDateTime.now());
        user.setKycConsentAccepted(true);
        user.setKycConsentAcceptedAt(LocalDateTime.now());
        user.setBankVerified(false);
        user.setBankVerifiedAt(null);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(servletRequest.getRemoteAddr());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy("FIREBASE_PHONE");
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID().toString());
        wallet.setUserId(user.getId());
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setTotalCredited(BigDecimal.ZERO);
        wallet.setTotalDebited(BigDecimal.ZERO);
        wallet.setVersionValue(0L);
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        createReferralLinks(user);
        auditService.log(user, "FIREBASE_MOBILE_REGISTERED", "User", user.getId(), null, verifiedPhone.mobileNumber(), servletRequest);

        Map<String, Object> response = authResponse(user);
        response.put("userExists", true);
        response.put("nextStep", nextOnboardingStep(user));
        response.put("message", "Registration successful. Complete KYC to continue onboarding.");
        return response;
    }

    @Transactional
    public Map<String, Object> mobileOnboardingRegister(ApiDtos.MobileOnboardingRegisterRequest request, HttpServletRequest servletRequest) {
        FirebasePhoneAuthService.VerifiedFirebasePhone verifiedPhone = firebasePhoneAuthService.verifyPhoneToken(request.idToken());
        if (userRepository.findByMobileNumber(verifiedPhone.mobileNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already exists");
        }
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        if (!request.termsAccepted() || !request.privacyPolicyAccepted() || !request.kycConsentAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terms, privacy policy, and KYC consent must be accepted");
        }
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setMobileNumber(verifiedPhone.mobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setReferralCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        user.setReferredByCode(request.referredByCode());
        user.setKycStatus(DomainEnums.KycStatus.NOT_SUBMITTED);
        user.setAccountStatus(DomainEnums.AccountStatus.PENDING);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.REGISTERED);
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setRiskDisclosureAccepted(true);
        user.setRiskDisclosureDate(LocalDateTime.now());
        user.setInvestorAgreementAccepted(true);
        user.setInvestorAgreementDate(LocalDateTime.now());
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setPrivacyPolicyAccepted(true);
        user.setPrivacyPolicyAcceptedAt(LocalDateTime.now());
        user.setKycConsentAccepted(true);
        user.setKycConsentAcceptedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(servletRequest.getRemoteAddr());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy("FIREBASE_PHONE_ONBOARDING");
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID().toString());
        wallet.setUserId(user.getId());
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setTotalCredited(BigDecimal.ZERO);
        wallet.setTotalDebited(BigDecimal.ZERO);
        wallet.setVersionValue(0L);
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        createReferralLinks(user);
        auditService.log(user, "MOBILE_ONBOARDING_REGISTERED", "User", user.getId(), null, verifiedPhone.mobileNumber(), servletRequest);

        Map<String, Object> response = authResponse(user);
        response.put("userExists", true);
        response.put("nextStep", "SET_MPIN");
        response.put("message", "Registration successful. Create MPIN to continue onboarding.");
        return response;
    }

    @Transactional
    public Map<String, Object> setMpin(User user, ApiDtos.SetMpinRequest request, HttpServletRequest servletRequest) {
        if (isWeakMpin(request.mpin())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MPIN cannot be a simple or repeated pattern");
        }
        user.setMpinHash(passwordEncoder.encode(request.mpin()));
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(user, "MPIN_CREATED", "User", user.getId(), null, "MPIN_CREATED", servletRequest);
        return onboardingResponse(user, "OPEN_DASHBOARD", "MPIN created successfully");
    }

    public Map<String, Object> verifyMpin(User user, ApiDtos.VerifyMpinRequest request, HttpServletRequest servletRequest) {
        validateMpin(user, request.mpin());
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(servletRequest.getRemoteAddr());
        userRepository.save(user);
        auditService.log(user, "MPIN_VERIFIED", "User", user.getId(), null, "MPIN_VERIFIED", servletRequest);
        return authResponse(user);
    }

    @Transactional
    public Map<String, Object> enableBiometric(User user, ApiDtos.EnableBiometricRequest request, HttpServletRequest servletRequest) {
        user.setBiometricEnabled(request.enabled());
        user.setBiometricDeviceId(request.enabled() ? request.deviceId() : null);
        user.setBiometricEnabledAt(request.enabled() ? LocalDateTime.now() : null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(user, request.enabled() ? "BIOMETRIC_ENABLED" : "BIOMETRIC_DISABLED", "User", user.getId(), null, request.deviceId(), servletRequest);
        return onboardingResponse(user, nextOnboardingStep(user), "Biometric preference updated");
    }

    @Transactional
    public Map<String, Object> verifyBank(User user, ApiDtos.VerifyBankRequest request, HttpServletRequest servletRequest) {
        if (!request.bankAccountNumber().equals(request.confirmBankAccountNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank account number and confirmation do not match");
        }
        if (user.getKycStatus() != DomainEnums.KycStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC must be approved before bank linking");
        }
        user.setBankAccountNumber(request.bankAccountNumber());
        user.setBankIfscCode(request.bankIfscCode());
        user.setBankName(request.bankName());
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            user.setFullName(request.accountHolderName());
        }
        user.setBankVerified(true);
        user.setBankVerifiedAt(LocalDateTime.now());
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.BANK_LINKED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(user, "BANK_VERIFIED", "User", user.getId(), null, request.bankIfscCode(), servletRequest);
        return onboardingResponse(user, nextOnboardingStep(user), "Bank account linked successfully");
    }

    @Transactional
    public Map<String, Object> activateOnboarding(User user, HttpServletRequest servletRequest) {
        if (!user.isTermsAccepted() || !user.isPrivacyPolicyAccepted() || !user.isKycConsentAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required legal consents are missing");
        }
        if (user.getKycStatus() != DomainEnums.KycStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC must be approved before activation");
        }
        if (!user.isBankVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank account must be verified before activation");
        }
        user.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.ACCOUNT_ACTIVATED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(user, "ACCOUNT_ACTIVATED", "User", user.getId(), null, "ACTIVE", servletRequest);
        return onboardingResponse(user, nextOnboardingStep(user), "Account activated successfully");
    }

    private void ensureMobileLoginAllowed(User user) {
        if (user.getAccountStatus() == DomainEnums.AccountStatus.SUSPENDED || user.getAccountStatus() == DomainEnums.AccountStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
    }

    private Map<String, Object> authResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        TokenRecord refreshToken = issueToken(user.getId(), DomainEnums.TokenType.REFRESH, refreshExpiryDays * 24);
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken.getTokenValue());
        response.put("role", user.getRole());
        response.put("userId", user.getId());
        response.put("kycStatus", user.getKycStatus());
        response.put("accountStatus", user.getAccountStatus());
        response.put("onboardingStatus", user.getOnboardingStatus());
        response.put("bankVerified", user.isBankVerified());
        response.put("mpinCreated", user.getMpinHash() != null && !user.getMpinHash().isBlank());
        return response;
    }

    private Map<String, Object> onboardingResponse(User user, String nextStep, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("nextStep", nextStep);
        response.put("userId", user.getId());
        response.put("accountStatus", user.getAccountStatus());
        response.put("onboardingStatus", user.getOnboardingStatus());
        response.put("kycStatus", user.getKycStatus());
        response.put("bankVerified", user.isBankVerified());
        response.put("mpinCreated", user.getMpinHash() != null && !user.getMpinHash().isBlank());
        response.put("biometricEnabled", user.isBiometricEnabled());
        return response;
    }

    private String nextOnboardingStep(User user) {
        if (user.getAccountStatus() == DomainEnums.AccountStatus.ACTIVE
                && user.getKycStatus() == DomainEnums.KycStatus.APPROVED
                && user.getMpinHash() != null
                && !user.getMpinHash().isBlank()) {
            return "OPEN_DASHBOARD";
        }
        if (user.getKycStatus() != DomainEnums.KycStatus.APPROVED) {
            return "COMPLETE_KYC";
        }
        if (!user.isBankVerified()) {
            return "VERIFY_BANK";
        }
        if (user.getAccountStatus() != DomainEnums.AccountStatus.ACTIVE) {
            return "ACTIVATE_ACCOUNT";
        }
        if (user.getMpinHash() == null || user.getMpinHash().isBlank()) {
            return "SET_MPIN";
        }
        return "OPEN_DASHBOARD";
    }

    private void validateMpin(User user, String mpin) {
        if (user.getMpinHash() == null || user.getMpinHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MPIN is not set");
        }
        if (!passwordEncoder.matches(mpin, user.getMpinHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MPIN");
        }
        if (user.getAccountStatus() == DomainEnums.AccountStatus.SUSPENDED || user.getAccountStatus() == DomainEnums.AccountStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
    }

    private boolean isWeakMpin(String mpin) {
        if (mpin.chars().distinct().count() == 1) {
            return true;
        }
        return "1234567890".contains(mpin) || "0987654321".contains(mpin);
    }

    public Map<String, Object> refresh(ApiDtos.RefreshTokenRequest request) {
        TokenRecord record = tokenRecordRepository.findByTokenValueAndTokenTypeAndUsedFalse(request.refreshToken(), DomainEnums.TokenType.REFRESH)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        return Map.of("accessToken", accessToken);
    }

    @Transactional
    public Map<String, Object> logout(User user, HttpServletRequest request) {
        tokenRecordRepository.findByUserIdAndTokenTypeAndUsedFalse(user.getId(), DomainEnums.TokenType.REFRESH)
                .forEach(token -> {
                    token.setUsed(true);
                    tokenRecordRepository.save(token);
                });
        auditService.log(user, "LOGOUT", "User", user.getId(), null, user.getEmail(), request);
        return Map.of("message", "Logged out successfully");
    }

    @Transactional
    public Map<String, Object> forgotPassword(ApiDtos.ForgotPasswordRequest request) {
        User user;
        if (request.email() != null && !request.email().isBlank()) {
            user = userRepository.findByEmail(request.email().toLowerCase())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        } else if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) {
            user = userRepository.findByMobileNumber(request.mobileNumber())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or mobile number is required");
        }
        TokenRecord token = issueToken(user.getId(), DomainEnums.TokenType.PASSWORD_RESET, 24);
        String resetLink = frontendBaseUrl.replaceAll("/+$", "") + "/reset-password?token=" + token.getTokenValue();
        emailService.sendPasswordReset(user.getEmail(), resetLink, token.getTokenValue());
        Map<String, Object> response = new HashMap<>();
        response.put("message", emailService.isEnabled() ? "Password reset email sent" : "Password reset token generated");
        if (exposeGeneratedValues) {
            response.put("resetToken", token.getTokenValue());
            response.put("resetLink", resetLink);
        }
        return response;
    }

    @Transactional
    public Map<String, Object> resetPassword(ApiDtos.ResetPasswordRequest request) {
        TokenRecord record = tokenRecordRepository.findByTokenValueAndTokenTypeAndUsedFalse(request.token(), DomainEnums.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset token"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token expired");
        }
        User user = userRepository.findById(record.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        record.setUsed(true);
        tokenRecordRepository.save(record);
        return Map.of("message", "Password reset successful");
    }

    @Transactional
    public Map<String, Object> changePassword(User user, ApiDtos.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return Map.of("message", "Password updated successfully");
    }

    private void ensureEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
    }

    private void ensureMobileAvailable(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }
        if (userRepository.findByMobileNumber(mobileNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number already exists");
        }
    }

    private TokenRecord issueOtp(String subject, DomainEnums.TokenType type, int expiryMinutes) {
        TokenRecord token = new TokenRecord();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(subject);
        token.setTokenValue(String.format("%06d", OTP_RANDOM.nextInt(1_000_000)));
        token.setTokenType(type);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        return tokenRecordRepository.save(token);
    }

    private Map<String, Object> verifyStoredOtp(String subject, String otp, DomainEnums.TokenType type, String verifiedStatus) {
        if (otp == null || otp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP is required");
        }
        TokenRecord record = tokenRecordRepository.findByUserIdAndTokenTypeAndUsedFalse(subject, type).stream()
                .filter(candidate -> otp.equals(candidate.getTokenValue()))
                .max((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP expired");
        }
        record.setUsed(true);
        tokenRecordRepository.save(record);
        TokenRecord verification = issueSignupVerification(subject);
        return Map.of(
                "message", "OTP verified successfully",
                "verifiedStatus", verifiedStatus,
                "signupVerificationToken", verification.getTokenValue(),
                "nextStep", "REGISTER"
        );
    }

    private TokenRecord issueSignupVerification(String subject) {
        TokenRecord token = new TokenRecord();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(subject);
        token.setTokenValue(UUID.randomUUID().toString() + UUID.randomUUID());
        token.setTokenType(DomainEnums.TokenType.SIGNUP_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        return tokenRecordRepository.save(token);
    }

    private void consumeSignupVerification(String signupVerificationToken, String email, String mobileNumber) {
        if (signupVerificationToken == null || signupVerificationToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verify email or mobile OTP before registration");
        }
        TokenRecord record = tokenRecordRepository.findByTokenValueAndTokenTypeAndUsedFalse(signupVerificationToken, DomainEnums.TokenType.SIGNUP_VERIFICATION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signup verification token"));
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signup verification token expired");
        }
        String subject = record.getUserId();
        boolean matchesEmail = email != null && subject.equalsIgnoreCase(email);
        boolean matchesMobile = mobileNumber != null && subject.equals(mobileNumber);
        if (!matchesEmail && !matchesMobile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signup verification token does not match email or mobile number");
        }
        record.setUsed(true);
        tokenRecordRepository.save(record);
    }

    private TokenRecord issueToken(String userId, DomainEnums.TokenType type, int expiryHours) {
        TokenRecord token = new TokenRecord();
        token.setId(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setTokenValue(UUID.randomUUID().toString() + UUID.randomUUID());
        token.setTokenType(type);
        token.setExpiresAt(LocalDateTime.now().plusHours(expiryHours));
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        return tokenRecordRepository.save(token);
    }

    private void createReferralLinks(User newUser) {
        if (newUser.getReferredByCode() == null || newUser.getReferredByCode().isBlank()) {
            return;
        }
        userRepository.findByReferralCode(newUser.getReferredByCode()).ifPresent(referrer -> {
            List<String> chain = new ArrayList<>();
            chain.add(referrer.getId());
            referralRelationshipRepository.findByReferredUserIdOrderByReferralLevelAsc(referrer.getId()).stream()
                    .filter(existing -> existing.getReferralLevel() < 5)
                    .forEach(existing -> chain.add(existing.getReferrerUserId()));
            for (int index = 0; index < chain.size() && index < 5; index++) {
                ReferralRelationship relationship = new ReferralRelationship();
                relationship.setId(UUID.randomUUID().toString());
                relationship.setReferrerUserId(chain.get(index));
                relationship.setReferredUserId(newUser.getId());
                relationship.setReferralLevel(index + 1);
                relationship.setActive(true);
                relationship.setLinkedAt(LocalDateTime.now());
                referralRelationshipRepository.save(relationship);
            }
        });
    }
}
