package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.AuthService;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final PlatformService platformService;
    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public KycController(PlatformService platformService, AuthService authService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping({"/pan/verify", "/pan-verify"})
    public Map<String, Object> verifyPan(@RequestBody ApiDtos.VerifyPanRequest request) {
        return authService.verifyPan(request);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('INVESTOR')")
    public Object submit(@RequestParam(value = "panCardImage", required = false) MultipartFile panCardImage,
                         @RequestParam(value = "aadhaarFrontImage", required = false) MultipartFile aadhaarFrontImage,
                         @RequestParam(value = "aadhaarBackImage", required = false) MultipartFile aadhaarBackImage,
                         @RequestParam(value = "selfiePhoto", required = false) MultipartFile selfiePhoto,
                         @RequestParam(value = "bankPassbookOrStatement", required = false) MultipartFile bankPassbookOrStatement,
                         HttpServletRequest request) throws IOException {
        return platformService.submitKyc(currentUserService.requireCurrentUser(), panCardImage, aadhaarFrontImage, aadhaarBackImage, selfiePhoto, bankPassbookOrStatement, request);
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('INVESTOR')")
    public Map<String, Object> status() {
        return platformService.getOwnKycStatus(currentUserService.requireCurrentUser());
    }
}

