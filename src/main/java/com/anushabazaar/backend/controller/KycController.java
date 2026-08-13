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

    @PostMapping("/pan/verify")
    public Map<String, Object> verifyPan(@RequestBody ApiDtos.VerifyPanRequest request) {
        return authService.verifyPan(request);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('INVESTOR')")
    public Object submit(@RequestParam("panCardImage") MultipartFile panCardImage,
                         @RequestParam("aadhaarFrontImage") MultipartFile aadhaarFrontImage,
                         @RequestParam("aadhaarBackImage") MultipartFile aadhaarBackImage,
                         @RequestParam("selfiePhoto") MultipartFile selfiePhoto,
                         @RequestParam("bankPassbookOrStatement") MultipartFile bankPassbookOrStatement,
                         HttpServletRequest request) throws IOException {
        return platformService.submitKyc(currentUserService.requireCurrentUser(), panCardImage, aadhaarFrontImage, aadhaarBackImage, selfiePhoto, bankPassbookOrStatement, request);
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('INVESTOR')")
    public Map<String, Object> status() {
        return platformService.getOwnKycStatus(currentUserService.requireCurrentUser());
    }
}

