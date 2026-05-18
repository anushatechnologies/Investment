package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/withdrawals")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminWithdrawalController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminWithdrawalController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/pending")
    public Object pending() {
        return platformService.getPendingWithdrawals();
    }

    @PostMapping("/{id}/approve")
    public Object approve(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.WithdrawalDecisionRequest request, HttpServletRequest servletRequest) {
        return platformService.approveWithdrawal(currentUserService.requireCurrentUser(), id,
                request == null ? new ApiDtos.WithdrawalDecisionRequest(null, null) : request, servletRequest);
    }

    @PostMapping("/{id}/process")
    public Object process(@PathVariable("id") String id, @RequestBody ApiDtos.WithdrawalProcessRequest request, HttpServletRequest servletRequest) {
        return platformService.processWithdrawal(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @PostMapping("/{id}/reject")
    public Object reject(@PathVariable("id") String id, @RequestBody ApiDtos.WithdrawalDecisionRequest request, HttpServletRequest servletRequest) {
        return platformService.rejectWithdrawal(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }
}
