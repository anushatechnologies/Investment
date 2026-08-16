package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@PreAuthorize("hasRole('INVESTOR')")
public class InvestorOpsController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public InvestorOpsController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/referrals/tree")
    public Object referralTree() {
        return platformService.getReferralTree(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/referrals/commissions")
    public Object commissions() {
        return platformService.getReferralCommissions(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/dashboard")
    public Object investorDashboard() {
        return platformService.getInvestorDashboard(currentUserService.requireCurrentUser());
    }

    /**
     * GET /api/statements
     * Returns transaction statements for the current user (wallet transactions + withdrawal history).
     * Called by Android dashboard.service.ts getStatements().
     */
    @GetMapping("/api/statements")
    public Object getStatements() {
        return platformService.getWalletTransactions(currentUserService.requireCurrentUser());
    }

    /**
     * GET /api/security/summary
     * Returns security-related status for the current user (2FA, biometric, MPIN, sessions).
     * Called by Android dashboard.service.ts getSecuritySummary().
     */
    @GetMapping("/api/security/summary")
    public Map<String, Object> getSecuritySummary() {
        var user = currentUserService.requireCurrentUser();
        return Map.of(
                "mpinConfigured", user.getMpinHash() != null,
                "biometricEnabled", user.isBiometricEnabled(),
                "emailVerified", user.isEmailVerified(),
                "bankVerified", user.isBankVerified(),
                "lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null,
                "lastLoginIp", user.getLastLoginIp() != null ? user.getLastLoginIp() : ""
        );
    }
}

