package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminOpsController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminOpsController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/admin/interest/rates")
    public Object interestRates() {
        return platformService.getInterestRates();
    }

    @PutMapping("/api/admin/interest/rates")
    public Object updateRate(@RequestParam("planId") String planId, @RequestBody ApiDtos.UpdateRateRequest request, HttpServletRequest servletRequest) {
        return platformService.updateInterestRate(currentUserService.requireCurrentUser(), planId, request, servletRequest);
    }

    @PostMapping("/api/admin/interest/trigger")
    public Object triggerInterest(HttpServletRequest request) {
        return platformService.triggerInterestRun(currentUserService.requireCurrentUser(), request);
    }

    @GetMapping("/api/admin/dashboard")
    public Object adminDashboard() {
        return platformService.getAdminDashboard();
    }

    @GetMapping("/api/admin/users")
    public Object users() {
        return platformService.getAllUsers();
    }

    @PutMapping("/api/admin/users/{id}")
    public Object updateUserStatus(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.UpdateUserStatusRequest request, HttpServletRequest servletRequest) {
        return platformService.updateUserStatus(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @PostMapping("/api/admin/users/{id}/suspend")
    public Object suspend(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.SuspendUserRequest request, HttpServletRequest servletRequest) {
        return platformService.suspendUser(currentUserService.requireCurrentUser(), id,
                request == null ? new ApiDtos.SuspendUserRequest("Suspended by admin") : request, servletRequest);
    }

    @GetMapping("/api/admin/fraud-alerts")
    public Object fraudAlerts() {
        return platformService.getFraudAlerts();
    }

    @PostMapping("/api/admin/fraud-alerts/{id}/resolve")
    public Object resolveFraud(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.ResolveAlertRequest request, HttpServletRequest servletRequest) {
        return platformService.resolveFraudAlert(currentUserService.requireCurrentUser(), id,
                request == null ? new ApiDtos.ResolveAlertRequest("Resolved", "RESOLVED") : request, servletRequest);
    }

    @GetMapping("/api/admin/audit-logs")
    public Object auditLogs(@RequestParam(value = "query", required = false) String query) {
        return platformService.getAuditLogs(query);
    }

    @GetMapping("/api/admin/reports/monthly")
    public Object monthlyReport() {
        return platformService.getMonthlyReport();
    }

    @GetMapping("/api/admin/referrals/report")
    public Object referralReport() {
        return platformService.getAdminReferralReport();
    }

    @GetMapping("/api/admin/referrals/commissions")
    public Object referralCommissionReview() {
        return platformService.getReferralCommissionReview();
    }

    @PostMapping("/api/admin/referrals/commissions/{commissionId}/release")
    public Object releaseReferralCommission(@PathVariable("commissionId") String commissionId, HttpServletRequest servletRequest) {
        return platformService.releaseHeldReferralCommission(currentUserService.requireCurrentUser(), commissionId, servletRequest);
    }

    @GetMapping("/api/admin/referrals/settings")
    public Object referralSettings() {
        return platformService.getReferralSettings();
    }

    @GetMapping("/api/admin/referrals/preview")
    public Object referralPreview(@RequestParam("investmentId") String investmentId) {
        return platformService.previewReferralPayoutForInvestment(investmentId);
    }

    @PostMapping("/api/admin/referrals/simulate")
    public Object referralSimulator(@RequestBody ApiDtos.ReferralPayoutSimulationRequest request) {
        return platformService.simulateReferralPayout(request);
    }

    @PutMapping("/api/admin/referrals/settings")
    public Object updateReferralSettings(@RequestBody ApiDtos.UpdateReferralSettingsRequest request, HttpServletRequest servletRequest) {
        return platformService.updateReferralSettings(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/api/admin/wallet/adjust")
    public Object adjustWallet(@RequestBody ApiDtos.AdminWalletAdjustmentRequest request, HttpServletRequest servletRequest) {
        return platformService.adminAdjustWallet(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @GetMapping("/api/admin/users/{id}/360")
    public Object user360(@PathVariable("id") String id) {
        return platformService.getAdminUser360(id);
    }

    @GetMapping("/api/admin/fraud/rules")
    public Object fraudRules() {
        return platformService.getFraudRuleSummary();
    }

    @GetMapping("/api/admin/support/tickets")
    public Object supportTickets() {
        return platformService.getAllSupportTickets();
    }

    @PostMapping("/api/admin/support/tickets/{id}/respond")
    public Object respondSupportTicket(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.RespondSupportTicketRequest body, HttpServletRequest request) {
        ApiDtos.RespondSupportTicketRequest resolved = body == null ? new ApiDtos.RespondSupportTicketRequest("IN_PROGRESS", null) : body;
        return platformService.respondSupportTicket(currentUserService.requireCurrentUser(), id, resolved, request);
    }
}
