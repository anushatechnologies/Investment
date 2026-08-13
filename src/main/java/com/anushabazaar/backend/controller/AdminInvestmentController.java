package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/investments")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminInvestmentController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminInvestmentController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/pending")
    public Object pending() {
        return platformService.getPendingInvestments();
    }

    @PostMapping("/{id}/verify-receipt")
    public Object verify(@PathVariable("id") String id, @RequestBody ApiDtos.VerifyReceiptRequest request, HttpServletRequest servletRequest) {
        return platformService.verifyReceipt(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @PostMapping("/{id}/activate")
    public Object activate(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.ActivateInvestmentRequest request, HttpServletRequest servletRequest) {
        return platformService.activateInvestment(currentUserService.requireCurrentUser(), id,
                request == null ? new ApiDtos.ActivateInvestmentRequest(null) : request, servletRequest);
    }

    @GetMapping
    public Object allInvestments() {
        return platformService.getAllInvestments();
    }

    @GetMapping("/{id}")
    public Object details(@PathVariable("id") String id) {
        return platformService.getAdminInvestmentDetails(id);
    }

    @PostMapping("/{id}/pause")
    public Object pause(@PathVariable("id") String id, HttpServletRequest servletRequest) {
        return platformService.pauseInvestmentByAdmin(currentUserService.requireCurrentUser(), id, servletRequest);
    }

    @PostMapping("/{id}/cancel")
    public Object cancel(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.CancelInvestmentRequest request, HttpServletRequest servletRequest) {
        String reason = request == null ? "Cancelled by admin" : request.reason();
        return platformService.cancelInvestmentByAdmin(currentUserService.requireCurrentUser(), id, reason, servletRequest);
    }
}
