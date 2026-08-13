package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ledger")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('FINANCE')")
public class AdminLedgerController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminLedgerController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Object getAllLedger() {
        return platformService.getAllLedgerTransactions();
    }

    @GetMapping("/transactions/{id}")
    public Object getLedgerTransaction(@PathVariable("id") String id) {
        return platformService.getLedgerTransaction(id);
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('FINANCE')")
    public Object adjustLedger(@RequestBody ApiDtos.AdminWalletAdjustRequest request, HttpServletRequest servletRequest) {
        return platformService.adjustLedgerBalance(currentUserService.requireCurrentUser(), request, servletRequest);
    }
}
