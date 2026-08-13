package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlanController {

    private final PlatformService platformService;
    private final CurrentUserService currentUserService;

    public PlanController(PlatformService platformService, CurrentUserService currentUserService) {
        this.platformService = platformService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/plans")
    @PreAuthorize("hasRole('INVESTOR')")
    public Object activePlans() {
        return platformService.getActivePlans();
    }

    @GetMapping("/api/admin/plans")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object allPlans() {
        return platformService.getAllPlans();
    }

    @PostMapping("/api/admin/plans")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object createPlan(@Valid @RequestBody ApiDtos.CreatePlanRequest request, HttpServletRequest servletRequest) {
        return platformService.createPlan(currentUserService.requireCurrentUser(), request, servletRequest);
    }

    @PutMapping("/api/admin/plans/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object updatePlan(@PathVariable("id") String id, @Valid @RequestBody ApiDtos.UpdatePlanRequest request, HttpServletRequest servletRequest) {
        return platformService.updatePlan(currentUserService.requireCurrentUser(), id, request, servletRequest);
    }

    @PostMapping("/api/admin/plans/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object deactivatePlan(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.deactivatePlan(currentUserService.requireCurrentUser(), id, request);
    }

    @PostMapping("/api/admin/plans/{id}/submit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object submitPlan(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.submitPlan(currentUserService.requireCurrentUser(), id, request);
    }

    @PostMapping("/api/admin/plans/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Object approvePlan(@PathVariable("id") String id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        String notes = body != null ? body.get("notes") : null;
        return platformService.approvePlan(currentUserService.requireCurrentUser(), id, notes, request);
    }

    @PostMapping("/api/admin/plans/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Object rejectPlan(@PathVariable("id") String id, @RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        String notes = body != null ? body.get("notes") : "Revision required";
        return platformService.rejectPlan(currentUserService.requireCurrentUser(), id, notes, request);
    }

    @PostMapping("/api/admin/plans/{id}/publish")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public Object publishPlan(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.publishPlan(currentUserService.requireCurrentUser(), id, request);
    }

    @PostMapping("/api/admin/plans/{id}/pause")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public Object pausePlan(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.pausePlan(currentUserService.requireCurrentUser(), id, request);
    }

    @PostMapping("/api/admin/plans/{id}/close")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Object closePlan(@PathVariable("id") String id, HttpServletRequest request) {
        return platformService.closePlan(currentUserService.requireCurrentUser(), id, request);
    }
}
