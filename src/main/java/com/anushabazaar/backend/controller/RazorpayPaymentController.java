package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class RazorpayPaymentController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public RazorpayPaymentController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @PostMapping({"/api/payments/razorpay/checkout-order", "/api/app/investments/orders"})
    @PreAuthorize("hasRole('INVESTOR')")
    public Object createCheckoutOrder(@Valid @RequestBody ApiDtos.ApplyInvestmentRequest request, HttpServletRequest servletRequest) {
        return platformService.createRazorpayCheckoutOrder(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping({"/api/payments/razorpay/verify", "/api/app/payments/verify"})
    @PreAuthorize("hasRole('INVESTOR')")
    public Object verifyPayment(@Valid @RequestBody ApiDtos.VerifyRazorpayPaymentRequest request, HttpServletRequest servletRequest) {
        return platformService.verifyRazorpayPayment(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @GetMapping({"/api/payments/razorpay/investments/{investmentId}", "/api/app/payments/{investmentId}/status"})
    @PreAuthorize("hasRole('INVESTOR')")
    public Object ownPayment(@PathVariable String investmentId) {
        return platformService.getOwnRazorpayPayment(currentUserService.requireCurrentUser(), investmentId);
    }

    @PostMapping({"/api/payments/razorpay/webhook", "/api/webhooks/razorpay"})
    public ResponseEntity<Object> webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                          @RequestHeader(value = "x-razorpay-event-id", required = false) String eventId,
                                          @RequestBody String payload) {
        return ResponseEntity.ok(platformService.handleRazorpayWebhook(signature, eventId, payload));
    }
}
