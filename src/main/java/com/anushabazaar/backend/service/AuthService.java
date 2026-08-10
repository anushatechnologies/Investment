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
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final ReferralRelationshipRepository referralRelationshipRepository;
    private final TokenRecordRepository tokenRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final int refreshExpiryDays;

    public AuthService(UserRepository userRepository,
                       WalletRepository walletRepository,
                       ReferralRelationshipRepository referralRelationshipRepository,
                       TokenRecordRepository tokenRecordRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       @Value("${app.jwt.refresh-expiry-days}") int refreshExpiryDays) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.referralRelationshipRepository = referralRelationshipRepository;
        this.tokenRecordRepository = tokenRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.refreshExpiryDays = refreshExpiryDays;
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
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setRiskDisclosureAccepted(true);
        user.setRiskDisclosureDate(LocalDateTime.now());
        user.setInvestorAgreementAccepted(true);
        user.setInvestorAgreementDate(LocalDateTime.now());
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
        response.put("message", "Registration successful. Verify email to activate account.");
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
        user.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        record.setUsed(true);
        tokenRecordRepository.save(record);
        auditService.log(user, "EMAIL_VERIFIED", "User", user.getId(), null, user.getEmail(), request);
        return Map.of("message", "Email verified successfully");
    }

    @Transactional
    public Map<String, Object> login(ApiDtos.LoginRequest request, HttpServletRequest servletRequest) {
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
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        TokenRecord token = issueToken(user.getId(), DomainEnums.TokenType.PASSWORD_RESET, 24);
        return Map.of("message", "Password reset token generated", "resetToken", token.getTokenValue());
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

    @Transactional
    public Map<String, Object> verifyBank(User user, ApiDtos.VerifyBankRequest request, HttpServletRequest servletRequest) {
        user.setBankAccountNumber(request.bankAccountNumber());
        user.setBankIfscCode(request.bankIfscCode());
        user.setBankName(request.bankName());
        user.setBankVerified(true);
        user.setBankVerifiedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditService.log(user, "BANK_VERIFIED", "User", user.getId(), null, request.bankAccountNumber(), servletRequest);
        return Map.of(
                "status", "VERIFIED",
                "message", "Bank details verified and linked successfully",
                "accountHolderName", user.getFullName(),
                "bankAccountNumber", request.bankAccountNumber(),
                "bankIfscCode", request.bankIfscCode(),
                "bankName", request.bankName()
        );
    }
}
