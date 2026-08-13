package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.AuthService;
import com.anushabazaar.backend.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AdminAuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public Map<String, Object> adminLogin(@RequestBody ApiDtos.AdminLoginRequest request, HttpServletRequest servletRequest) {
        return authService.adminLogin(request, servletRequest);
    }

    @PostMapping("/verify-2fa")
    public Map<String, Object> verify2fa(@RequestBody ApiDtos.Verify2faRequest request, HttpServletRequest servletRequest) {
        return authService.verifyAdmin2fa(request, servletRequest);
    }

    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestBody ApiDtos.RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> logout(HttpServletRequest request) {
        return authService.logout(currentUserService.requireCurrentUser(), request);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getAdminMe() {
        User admin = currentUserService.requireCurrentUser();
        return authService.getAdminProfile(admin);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<User> getAdminStaff() {
        return authService.getAdminStaff();
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public User createAdminStaff(@RequestBody ApiDtos.CreateAdminStaffRequest request, HttpServletRequest servletRequest) {
        return authService.createAdminStaff(request, servletRequest);
    }
}
