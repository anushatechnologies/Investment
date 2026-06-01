package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class CouponController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public CouponController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/coupons")
    @PreAuthorize("hasRole('INVESTOR')")
    public Object activeCoupons() {
        return platformService.getActiveCouponsForInvestor();
    }

    @PostMapping("/api/coupons/validate")
    @PreAuthorize("hasRole('INVESTOR')")
    public Object validateCoupon(@Valid @RequestBody ApiDtos.ValidateCouponRequest request) {
        return platformService.validateCoupon(currentUserService.requireCurrentUser(), request);
    }

    @GetMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object allCoupons() {
        return platformService.getAllCoupons();
    }

    @PostMapping("/api/admin/coupons")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object createCoupon(@Valid @RequestBody ApiDtos.CreateCouponRequest request, HttpServletRequest servletRequest) {
        return platformService.createCoupon(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PutMapping("/api/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object updateCoupon(@PathVariable("id") String id, @Valid @RequestBody ApiDtos.UpdateCouponRequest request, HttpServletRequest servletRequest) {
        return platformService.updateCoupon(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }
}
