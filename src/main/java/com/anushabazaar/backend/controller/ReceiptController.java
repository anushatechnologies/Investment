package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.ReceiptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final CurrentUserService currentUserService;

    public ReceiptController(ReceiptService receiptService, CurrentUserService currentUserService) {
        this.receiptService = receiptService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/{investmentId}/status")
    @PreAuthorize("isAuthenticated()")
    public ApiDtos.ReceiptStatusResponse getReceiptStatus(@PathVariable("investmentId") String investmentId) {
        return receiptService.getReceiptStatus(currentUserService.requireCurrentUser(), investmentId);
    }

    @GetMapping(value = {"/{investmentId}/invoice", "/{investmentId}/download"}, produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> invoice(@PathVariable("investmentId") String investmentId) {
        return ResponseEntity.ok(receiptService.renderInvoice(currentUserService.requireCurrentUser(), investmentId));
    }
}
