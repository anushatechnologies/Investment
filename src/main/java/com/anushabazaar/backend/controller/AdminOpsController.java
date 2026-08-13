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
