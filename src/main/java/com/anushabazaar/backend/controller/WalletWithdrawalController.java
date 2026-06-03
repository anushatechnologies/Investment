package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('INVESTOR')")
public class WalletWithdrawalController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public WalletWithdrawalController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/wallet")
    public Object wallet() {
        return platformService.getWallet(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/wallet/transactions")
    public Object walletTransactions() {
        return platformService.getWalletTransactions(currentUserService.requireCurrentUser());
    }

    @GetMapping("/api/wallet/transactions/{id}/proof")
    public Object walletTransactionProof(@PathVariable("id") String id) {
        return platformService.getWalletTransactionProof(currentUserService.requireCurrentUser(), id);
    }

    @GetMapping("/api/withdrawals/settings")
    public Object withdrawalSettings() {
        return platformService.getWithdrawalSettings();
    }

    @PostMapping("/api/withdrawals/request")
    public Object requestWithdrawal(@Valid @RequestBody ApiDtos.RequestWithdrawalRequest request, HttpServletRequest servletRequest) {
        return platformService.requestWithdrawal(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @GetMapping("/api/withdrawals")
    public Object ownWithdrawals() {
        return platformService.getOwnWithdrawals(currentUserService.requireCurrentUser());
    }
}
