package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/kyc")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminKycController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminKycController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/pending")
    public Object pending() {
        return platformService.getPendingKyc();
    }

    @PostMapping("/{id}/approve")
    public Object approve(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.KycDecisionRequest request, HttpServletRequest servletRequest) {
        String notes = request == null ? null : request.adminNotes();
        return platformService.approveKyc(currentUserService.requireCurrentUser(), id, notes, servletRequest);
    }

    @PostMapping("/{id}/reject")
    public Object reject(@PathVariable("id") String id, @RequestBody ApiDtos.KycDecisionRequest request, HttpServletRequest servletRequest) {
        return platformService.rejectKyc(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @PostMapping("/{id}/documents/reject")
    public Object rejectDocuments(@PathVariable("id") String id,
                                  @RequestBody ApiDtos.KycDocumentRejectionRequest request,
                                  HttpServletRequest servletRequest) {
        return platformService.rejectKycDocuments(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @GetMapping("/{id}/documents")
    public Object documents(@PathVariable("id") String id) {
        return platformService.getKycDocuments(id);
    }
}
