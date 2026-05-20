package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
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
                         @RequestParam(value = "panNumber", required = false) String panNumber,
                         @RequestParam(value = "aadhaarLast4", required = false) String aadhaarLast4,
                         @RequestParam(value = "dateOfBirth", required = false) LocalDate dateOfBirth,
                         @RequestParam(value = "address", required = false) String address,
                         HttpServletRequest request) {
        return platformService.submitKyc(currentUserService.requireCurrentUser(), panCardImage, aadhaarFrontImage,
                aadhaarBackImage, selfiePhoto, bankPassbookOrStatement, panNumber, aadhaarLast4, dateOfBirth, address, request);
    }

    @PostMapping("/pan-verify")
    public Object panVerify(@RequestParam("panCardImage") MultipartFile panCardImage,
                            @RequestParam("panNumber") String panNumber,
                            HttpServletRequest request) {
        return platformService.submitPan(currentUserService.requireCurrentUser(), panCardImage, panNumber, request);
    }

    @PostMapping("/aadhaar-verify")
    public Object aadhaarVerify(@RequestParam("aadhaarFrontImage") MultipartFile aadhaarFrontImage,
                                @RequestParam("aadhaarBackImage") MultipartFile aadhaarBackImage,
                                @RequestParam(value = "aadhaarNumber", required = false) String aadhaarNumber,
                                @RequestParam(value = "aadhaarLast4", required = false) String aadhaarLast4,
                                @RequestParam(value = "address", required = false) String address,
                                HttpServletRequest request) {
        return platformService.submitAadhaar(currentUserService.requireCurrentUser(), aadhaarFrontImage, aadhaarBackImage,
                aadhaarNumber, aadhaarLast4, address, request);
    }

    @PostMapping("/upload-selfie")
    public Object uploadSelfie(@RequestParam("selfiePhoto") MultipartFile selfiePhoto,
                               HttpServletRequest request) {
        return platformService.uploadSelfie(currentUserService.requireCurrentUser(), selfiePhoto, request);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return platformService.getOwnKycStatus(currentUserService.requireCurrentUser());
    }
}
