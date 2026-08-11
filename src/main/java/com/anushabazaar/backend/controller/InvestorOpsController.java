package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
