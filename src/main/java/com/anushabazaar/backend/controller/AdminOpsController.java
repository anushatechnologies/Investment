package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.SupportTicket;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.repository.SupportTicketRepository;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminOpsController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;
    private final SupportTicketRepository supportTicketRepository;

    public AdminOpsController(PlatformService platformService, CurrentUserService currentUserService, SupportTicketRepository supportTicketRepository) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
        this.supportTicketRepository = supportTicketRepository;
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

    @GetMapping("/api/admin/users/{id}/360")
    public Object getUser360(@PathVariable("id") String id) {
        return platformService.getUser360(id);
    }

    @PostMapping("/api/admin/users/{id}/suspend")
    public Object suspend(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.SuspendUserRequest request, HttpServletRequest servletRequest) {
        return platformService.suspendUser(currentUserService.requireCurrentUser(), id,
                request == null ? new ApiDtos.SuspendUserRequest("Suspended by admin") : request, servletRequest);
    }

    @PostMapping("/api/admin/users/{id}/block")
    public Object block(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.SuspendUserRequest request, HttpServletRequest servletRequest) {
        return platformService.blockUser(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @PostMapping("/api/admin/users/{id}/unblock")
    public Object unblock(@PathVariable("id") String id, HttpServletRequest servletRequest) {
        return platformService.unblockUser(currentUserService.requireCurrentUser(), id, servletRequest);
    }

    @GetMapping("/api/admin/fraud-alerts")
    public Object fraudAlerts() {
        return platformService.getFraudAlerts();
    }

    @GetMapping("/api/admin/fraud/rules")
    public Object fraudRules() {
        return platformService.getFraudRules();
    }

    @PostMapping("/api/admin/fraud-alerts/{id}/resolve")
    public Object resolveFraud(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.ResolveAlertRequest request, HttpServletRequest servletRequest) {
        return platformService.resolveFraudAlert(currentUserService.requireCurrentUser(), id,
                request == null ? new ApiDtos.ResolveAlertRequest("Resolved", "RESOLVED") : request, servletRequest);
    }

    @GetMapping("/api/admin/referrals/report")
    public Object referralReport() {
        return platformService.getReferralReport();
    }

    @GetMapping("/api/admin/referrals/settings")
    public Object referralSettings() {
        return platformService.getReferralSettings();
    }

    @PutMapping("/api/admin/referrals/settings")
    public Object updateReferralSettings(@RequestBody Map<String, Object> settings, HttpServletRequest servletRequest) {
        return platformService.updateReferralSettings(currentUserService.requireCurrentUser(), settings, servletRequest);
    }

    @GetMapping("/api/admin/referrals/commissions")
    public Object referralCommissions() {
        return platformService.getReferralCommissionsForAdmin();
    }

    @PostMapping("/api/admin/referrals/commissions/{id}/release")
    public Object releaseReferralCommission(@PathVariable("id") String id, HttpServletRequest servletRequest) {
        return platformService.releaseReferralCommission(currentUserService.requireCurrentUser(), id, servletRequest);
    }

    @GetMapping("/api/admin/referrals/preview")
    public Object previewReferral(@RequestParam("investmentId") String investmentId) {
        return platformService.previewReferralPayout(investmentId);
    }

    @PostMapping("/api/admin/referrals/simulate")
    public Object simulateReferral(@RequestBody Map<String, Object> payload) {
        return platformService.simulateReferralPayout(payload);
    }

    @GetMapping("/api/admin/support/tickets")
    public Object supportTickets() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/api/admin/support/tickets/{id}/respond")
    public Object respondSupportTicket(@PathVariable("id") String id, @RequestBody(required = false) Map<String, String> payload, HttpServletRequest servletRequest) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        String response = payload != null ? payload.get("response") : null;
        if (response != null && !response.isBlank()) {
            ticket.setAdminReply(response.trim());
        }
        ticket.setRespondedByAdminId(currentUserService.requireCurrentUser().getId());
        ticket.setStatus(DomainEnums.SupportTicketStatus.RESOLVED);
        ticket.setUpdatedAt(LocalDateTime.now());
        return supportTicketRepository.save(ticket);
    }

    @GetMapping("/api/admin/audit-logs")
    public Object auditLogs(@RequestParam(value = "query", required = false) String query) {
        return platformService.getAuditLogs(query);
    }

    @GetMapping("/api/admin/reports/monthly")
    public Object monthlyReport() {
        return platformService.getMonthlyReport();
    }

    @GetMapping("/api/admin/payouts")
    public Object payouts() {
        return platformService.getPayouts();
    }

    @PostMapping("/api/admin/payouts/calculate")
    public Object calculatePayouts(HttpServletRequest request) {
        return platformService.triggerInterestRun(currentUserService.requireCurrentUser(), request);
    }

    @GetMapping("/api/admin/maturity/upcoming")
    public Object upcomingMaturities() {
        return platformService.getUpcomingMaturities();
    }

    @PostMapping("/api/admin/maturity/{id}/settle")
    public Object settleMaturity(@PathVariable("id") String id, HttpServletRequest servletRequest) {
        return platformService.settleMaturity(currentUserService.requireCurrentUser(), id, servletRequest);
    }
}
