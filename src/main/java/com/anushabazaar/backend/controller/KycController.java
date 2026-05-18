package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@PreAuthorize("hasRole('INVESTOR')")
public class KycController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public KycController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/submit")
    public Object submit(@RequestParam("panCardImage") MultipartFile panCardImage,
                         @RequestParam("aadhaarFrontImage") MultipartFile aadhaarFrontImage,
                         @RequestParam("aadhaarBackImage") MultipartFile aadhaarBackImage,
                         @RequestParam("selfiePhoto") MultipartFile selfiePhoto,
                         @RequestParam("bankPassbookOrStatement") MultipartFile bankPassbookOrStatement,
                         HttpServletRequest request) {
        return platformService.submitKyc(currentUserService.requireCurrentUser(), panCardImage, aadhaarFrontImage, aadhaarBackImage, selfiePhoto, bankPassbookOrStatement, request);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return platformService.getOwnKycStatus(currentUserService.requireCurrentUser());
    }
}
