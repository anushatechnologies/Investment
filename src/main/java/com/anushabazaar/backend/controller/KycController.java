package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    public Object submit(@RequestParam(value = "panCardImage", required = false) MultipartFile panCardImage,
                         @RequestParam(value = "aadhaarFrontImage", required = false) MultipartFile aadhaarFrontImage,
                         @RequestParam(value = "aadhaarBackImage", required = false) MultipartFile aadhaarBackImage,
                         @RequestParam(value = "selfiePhoto", required = false) MultipartFile selfiePhoto,
                         @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                         @RequestParam(value = "PROFILE_PHOTO", required = false) MultipartFile profilePhotoUpper,
                         @RequestParam(value = "bankPassbookOrStatement", required = false) MultipartFile bankPassbookOrStatement,
                         @RequestParam(value = "panNumber", required = false) String panNumber,
                         @RequestParam(value = "aadhaarLast4", required = false) String aadhaarLast4,
                         @RequestParam(value = "dateOfBirth", required = false) LocalDate dateOfBirth,
                         @RequestParam(value = "address", required = false) String address,
                         HttpServletRequest request) {
        return platformService.submitKyc(currentUserService.requireCurrentUser(), panCardImage, aadhaarFrontImage,
                aadhaarBackImage, firstPresentOptional(selfiePhoto, profilePhoto, profilePhotoUpper),
                bankPassbookOrStatement, panNumber, aadhaarLast4, dateOfBirth, address, request);
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
    public Object uploadSelfie(@RequestParam(value = "selfiePhoto", required = false) MultipartFile selfiePhoto,
                               @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                               @RequestParam(value = "PROFILE_PHOTO", required = false) MultipartFile profilePhotoUpper,
                               HttpServletRequest request) {
        return platformService.uploadSelfie(currentUserService.requireCurrentUser(),
                firstPresent(selfiePhoto, profilePhoto, profilePhotoUpper, "selfie/profile photo"), request);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return platformService.getOwnKycStatus(currentUserService.requireCurrentUser());
    }

    private MultipartFile firstPresent(MultipartFile primary, MultipartFile secondary, MultipartFile tertiary, String label) {
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        if (secondary != null && !secondary.isEmpty()) {
            return secondary;
        }
        if (tertiary != null && !tertiary.isEmpty()) {
            return tertiary;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
    }

    private MultipartFile firstPresentOptional(MultipartFile primary, MultipartFile secondary, MultipartFile tertiary) {
        if (primary != null && !primary.isEmpty()) {
            return primary;
        }
        if (secondary != null && !secondary.isEmpty()) {
            return secondary;
        }
        if (tertiary != null && !tertiary.isEmpty()) {
            return tertiary;
        }
        return null;
    }
}
