package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/payments/razorpay")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminRazorpayController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminRazorpayController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Object payments() {
        return platformService.getAllRazorpayPayments();
    }

    @GetMapping("/settlements")
    public Object settlements(@RequestParam(value = "count", required = false) Integer count,
                              @RequestParam(value = "skip", required = false) Integer skip) {
        return platformService.getRazorpaySettlements(count, skip);
    }

    @PostMapping("/investments/{investmentId}/sync")
    public Object syncInvestmentPayment(@PathVariable String investmentId, HttpServletRequest request) {
        return platformService.syncRazorpayPayment(currentUserService.requireCurrentUser(), investmentId, request);
    }
}
