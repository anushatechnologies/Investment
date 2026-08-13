package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
public class AdminSettingsController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public AdminSettingsController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Object getSettings() {
        return platformService.getSystemSettings();
    }

    @PutMapping
    public Object updateSettings(@RequestBody Map<String, Object> settings, HttpServletRequest request) {
        return platformService.updateSystemSettings(currentUserService.requireCurrentUser(), settings, request);
    }
}
