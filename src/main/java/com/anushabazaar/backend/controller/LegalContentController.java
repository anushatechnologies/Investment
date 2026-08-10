package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LegalContentController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public LegalContentController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/legal/{documentKey}")
    public Object publicLegalDocument(@PathVariable("documentKey") String documentKey) {
        return platformService.getLegalDocument(documentKey);
    }

    @GetMapping("/api/admin/legal")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object adminLegalDocuments() {
        return platformService.getLegalDocuments();
    }

    @GetMapping("/api/admin/legal/{documentKey}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object adminLegalDocument(@PathVariable("documentKey") String documentKey) {
        return platformService.getLegalDocument(documentKey);
    }

    @PutMapping("/api/admin/legal/{documentKey}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object updateLegalDocument(@PathVariable("documentKey") String documentKey,
                                      @Valid @RequestBody ApiDtos.UpdateLegalDocumentRequest body,
                                      HttpServletRequest request) {
        return platformService.updateLegalDocument(currentUserService.requireCurrentUser(), documentKey, body, request);
    }
}
