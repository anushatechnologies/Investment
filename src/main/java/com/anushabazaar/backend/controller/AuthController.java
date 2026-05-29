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
    public Map<String, Object> register(@Valid @RequestBody ApiDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        return authService.register(request, servletRequest);
    }

    @GetMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestParam("token") String token, HttpServletRequest request) {
        return authService.verifyEmail(token, request);
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody ApiDtos.LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request, servletRequest);
    }

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@Valid @RequestBody ApiDtos.SendOtpRequest request) {
        return authService.sendOtp(request);
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@Valid @RequestBody ApiDtos.VerifyOtpRequest request,
                                         HttpServletRequest servletRequest) {
        return authService.verifyOtp(request, servletRequest);
    }

    @GetMapping("/referrals/validate")
    public Map<String, Object> validateReferral(@RequestParam("code") String code) {
        return authService.validateReferralCode(code);
    }

    @PostMapping("/firebase-mobile/login")
    public Map<String, Object> firebaseMobileLogin(@Valid @RequestBody ApiDtos.FirebaseMobileLoginRequest request,
                                                   HttpServletRequest servletRequest) {
        return authService.firebaseMobileLogin(request, servletRequest);
    }

    @PostMapping("/firebase-mobile/register")
    public Map<String, Object> firebaseMobileRegister(@Valid @RequestBody ApiDtos.FirebaseMobileRegisterRequest request,
                                                      HttpServletRequest servletRequest) {
        return authService.firebaseMobileRegister(request, servletRequest);
    }

    @PostMapping("/onboarding/register")
    public Map<String, Object> mobileOnboardingRegister(@Valid @RequestBody ApiDtos.MobileOnboardingRegisterRequest request,
                                                        HttpServletRequest servletRequest) {
        return authService.mobileOnboardingRegister(request, servletRequest);
    }

    @PostMapping("/set-mpin")
    public Map<String, Object> setMpin(@Valid @RequestBody ApiDtos.SetMpinRequest request,
                                       HttpServletRequest servletRequest) {
        return authService.setMpin(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/verify-mpin")
    public Map<String, Object> verifyMpin(@Valid @RequestBody ApiDtos.VerifyMpinRequest request,
                                          HttpServletRequest servletRequest) {
        return authService.verifyMpin(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/enable-biometric")
    public Map<String, Object> enableBiometric(@Valid @RequestBody ApiDtos.EnableBiometricRequest request,
                                               HttpServletRequest servletRequest) {
        return authService.enableBiometric(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/activate")
    public Map<String, Object> activate(@Valid @RequestBody(required = false) Map<String, Object> ignored,
                                        HttpServletRequest servletRequest) {
        return authService.activateOnboarding(currentUserService.requireCurrentUser(), servletRequest);
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
}
