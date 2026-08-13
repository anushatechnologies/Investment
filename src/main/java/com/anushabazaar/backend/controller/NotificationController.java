package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public NotificationController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Object notifications() {
        return platformService.getNotifications(currentUserService.requireCurrentUser());
    }

    @GetMapping("/preferences")
    public Object preferences() {
        return platformService.getNotificationPreferences(currentUserService.requireCurrentUser());
    }

    @PutMapping("/preferences")
    public Object updatePreferences(@RequestBody ApiDtos.UpdateNotificationPreferencesRequest request, HttpServletRequest servletRequest) {
        return platformService.updateNotificationPreferences(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @GetMapping("/summary")
    public Object summary() {
        return platformService.getNotificationSummary(currentUserService.requireCurrentUser());
    }

    @PostMapping("/{id}/read")
    public Object markRead(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.markNotificationRead(currentUserService.requireCurrentUser(), id, request);
    }

    @PostMapping("/read-all")
    public Object markAllRead(HttpServletRequest request) {
        return platformService.markAllNotificationsRead(currentUserService.requireCurrentUser(), request);
    }

    @DeleteMapping("/{id}")
    public Object delete(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.deleteNotification(currentUserService.requireCurrentUser(), id, request);
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object broadcast(@RequestBody ApiDtos.BroadcastNotificationRequest request, HttpServletRequest servletRequest) {
        return platformService.broadcastNotification(currentUserService.requireCurrentUser(), request, servletRequest);
    }
}
