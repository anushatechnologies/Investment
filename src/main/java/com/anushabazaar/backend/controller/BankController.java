package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.AuthService;
import com.anushabazaar.backend.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/bank")
@PreAuthorize("hasRole('INVESTOR')")
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
}
