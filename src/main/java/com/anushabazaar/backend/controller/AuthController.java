package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.AuthService;
import com.anushabazaar.backend.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody ApiDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        return authService.register(request, servletRequest);
    }

    @PostMapping("/onboarding/register")
    public Map<String, Object> onboardingRegister(@RequestBody ApiDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        return authService.register(request, servletRequest);
    }

    @PostMapping("/onboarding/register-investor")
    public Map<String, Object> onboardingRegisterInvestor(@RequestBody ApiDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        return authService.register(request, servletRequest);
    }

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@RequestBody ApiDtos.SendOtpRequest request, HttpServletRequest servletRequest) {
        return authService.sendOtp(request, servletRequest);
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@RequestBody ApiDtos.VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @PostMapping("/onboarding/send-otp")
    public Map<String, Object> onboardingSendOtp(@RequestBody ApiDtos.SendOtpRequest request, HttpServletRequest servletRequest) {
        return authService.sendOtp(request, servletRequest);
    }

    @PostMapping("/onboarding/verify-otp")
    public Map<String, Object> onboardingVerifyOtp(@RequestBody ApiDtos.VerifyOtpRequest request) {
        return authService.verifyOtp(request);
    }

    @GetMapping("/onboarding/status")
    public Map<String, Object> onboardingStatus() {
        return Map.of("status", "PENDING_KYC", "onboardingStep", "KYC");
    }

    @GetMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestParam("token") String token, HttpServletRequest request) {
        return authService.verifyEmail(token, request);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody ApiDtos.LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request, servletRequest);
    }

    @PostMapping("/mobile-login")
    public Map<String, Object> mobileLogin(@RequestBody ApiDtos.MobileLoginRequest request, HttpServletRequest servletRequest) {
        return authService.mobileLogin(request, servletRequest);
    }

    @PostMapping("/mpin-login")
    public Map<String, Object> mpinLogin(@RequestBody ApiDtos.MobileLoginRequest request, HttpServletRequest servletRequest) {
        return authService.mobileLogin(request, servletRequest);
    }

    @PostMapping("/set-mpin")
    public Map<String, Object> setMpin(@RequestBody ApiDtos.SetMpinRequest request, HttpServletRequest servletRequest) {
        return authService.setMpin(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/verify-mpin")
    public Map<String, Object> verifyMpin(@RequestBody ApiDtos.SetMpinRequest request) {
        return authService.verifyMpin(currentUserService.requireCurrentUser(), request);
    }

    @PostMapping("/refresh-token")
    public Map<String, Object> refresh(@Valid @RequestBody ApiDtos.RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        return authService.logout(currentUserService.requireCurrentUser(), request);
    }

    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ApiDtos.ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@Valid @RequestBody ApiDtos.ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@Valid @RequestBody ApiDtos.ChangePasswordRequest request) {
        return authService.changePassword(currentUserService.requireCurrentUser(), request);
    }

    /**
     * POST /api/auth/activate
     * Activates a newly registered account after KYC and bank verification.
     * Called by Android app after onboarding completion.
     */
    @PostMapping("/activate")
    public Map<String, Object> activateAccount() {
        return authService.activateAccount(currentUserService.requireCurrentUser());
    }

    /**
     * POST /api/auth/enable-biometric
     * Enables or disables biometric authentication for the user's device.
     * Called by Android app from biometric settings screen.
     */
    @PostMapping("/enable-biometric")
    public Map<String, Object> enableBiometric(@RequestBody Map<String, Object> body) {
        return authService.setBiometricPreference(currentUserService.requireCurrentUser(), body);
    }

    /**
     * GET /api/auth/referrals/validate?code=XXX
     * Validates a referral code entered during signup.
     */
    @GetMapping("/referrals/validate")
    public Map<String, Object> validateReferralCode(@RequestParam("code") String code) {
        return authService.validateReferralCode(code);
    }
}
