package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.AuthService;
import com.anushabazaar.backend.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bank")
public class BankController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public BankController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyBank(@Valid @RequestBody ApiDtos.VerifyBankRequest request,
                                          HttpServletRequest servletRequest) {
        return authService.verifyBank(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/link")
    public Map<String, Object> linkBank(@Valid @RequestBody ApiDtos.VerifyBankRequest request,
                                        HttpServletRequest servletRequest) {
        return authService.verifyBank(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @GetMapping("/details")
    public Map<String, Object> details() {
        var user = currentUserService.requireCurrentUser();
        return Map.of(
                "accountHolderName", user.getFullName(),
                "bankAccountNumber", user.getBankAccountNumber() == null ? "" : user.getBankAccountNumber(),
                "bankIfscCode", user.getBankIfscCode() == null ? "" : user.getBankIfscCode(),
                "bankName", user.getBankName() == null ? "" : user.getBankName(),
                "bankVerified", user.isBankVerified(),
                "bankVerifiedAt", user.getBankVerifiedAt() == null ? "" : user.getBankVerifiedAt()
        );
    }

    @PutMapping("/update")
    public Map<String, Object> updateBank(@RequestBody ApiDtos.UpdateBankRequest request,
                                          HttpServletRequest servletRequest) {
        return authService.updateBankDetails(currentUserService.requireCurrentUser(), request, true, servletRequest);
    }

    @PatchMapping("/update")
    public Map<String, Object> patchBank(@RequestBody ApiDtos.UpdateBankRequest request,
                                         HttpServletRequest servletRequest) {
        return authService.updateBankDetails(currentUserService.requireCurrentUser(), request, false, servletRequest);
    }
}
