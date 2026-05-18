package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.service.CurrentUserService;
import com.anushabazaar.backend.service.PlatformService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
}
