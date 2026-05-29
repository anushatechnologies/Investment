package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping("/api/notifications")
    public Object notifications() {
        return platformService.getNotifications(currentUserService.requireCurrentUser());
    }

    @PostMapping("/api/notifications/{id}/read")
    public Object markRead(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.markNotificationRead(currentUserService.requireCurrentUser(), id, request);
    }

    @GetMapping("/api/dashboard")
    public Object investorDashboard() {
        return platformService.getInvestorDashboard(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/statements")
    public Object statements() {
        return platformService.getInvestorStatements(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/security/summary")
    public Object securitySummary() {
        return platformService.getSecuritySummary(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/support/tickets")
    public Object supportTickets() {
        return platformService.getOwnSupportTickets(currentUserService.requireCurrentUser());
    }

    @PostMapping("/api/support/tickets")
    public Object createSupportTicket(@RequestBody ApiDtos.CreateSupportTicketRequest body, HttpServletRequest request) {
        return platformService.createSupportTicket(currentUserService.requireCurrentUser(), body, request);
    }
}
