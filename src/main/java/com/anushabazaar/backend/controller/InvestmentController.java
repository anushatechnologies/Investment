package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/investments")
@PreAuthorize("hasRole('INVESTOR')")
public class InvestmentController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public InvestmentController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/apply")
    public Object apply(@Valid @RequestBody ApiDtos.ApplyInvestmentRequest request, HttpServletRequest servletRequest) {
        return platformService.applyInvestment(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PostMapping("/{id}/upload-receipt")
    public Object uploadReceipt(@PathVariable("id") String id,
                                @RequestParam("receiptFile") MultipartFile receiptFile,
                                @RequestParam("paymentAmount") BigDecimal paymentAmount,
                                @RequestParam("paymentDate") LocalDate paymentDate,
                                @RequestParam("paymentMode") DomainEnums.PaymentMode paymentMode,
                                @RequestParam("bankReference") String bankReference,
                                HttpServletRequest request) {
        return platformService.uploadReceipt(currentUserService.requireCurrentUser(), id, receiptFile, paymentAmount, paymentDate, paymentMode, bankReference, request);
    }

    @GetMapping
    public Object ownInvestments() {
        return platformService.getOwnInvestments(currentUserService.requireCurrentUser());
    }

    @GetMapping("/{id}")
    public Object ownInvestment(@PathVariable("id") String id) {
        return platformService.getOwnInvestment(currentUserService.requireCurrentUser(), id);
    }

    @PostMapping("/{id}/cancel")
    public Object cancel(@PathVariable("id") String id, @RequestBody(required = false) ApiDtos.CancelInvestmentRequest request, HttpServletRequest servletRequest) {
        return platformService.cancelInvestment(currentUserService.requireCurrentUser(), id, request == null ? new ApiDtos.CancelInvestmentRequest("Cancelled by investor") : request, servletRequest);
    }
}
