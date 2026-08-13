package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.SupportTicket;
import com.anushabazaar.backend.repository.SupportTicketRepository;
import com.anushabazaar.backend.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@PreAuthorize("isAuthenticated()")
public class SupportTicketController {

    private final SupportTicketRepository supportTicketRepository;
    private final CurrentUserService currentUserService;

    public SupportTicketController(SupportTicketRepository supportTicketRepository,
                                   CurrentUserService currentUserService) {
        this.supportTicketRepository = supportTicketRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * GET /api/support/tickets
     * Returns all support tickets belonging to the currently authenticated user.
     */
    @GetMapping("/tickets")
    public List<SupportTicket> listOwnTickets() {
        String userId = currentUserService.requireCurrentUser().getId();
        return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * POST /api/support/tickets
     * Creates a new support ticket for the currently authenticated user.
     */
    @PostMapping("/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportTicket createTicket(@RequestBody Map<String, String> body) {
        String userId = currentUserService.requireCurrentUser().getId();

        String category = body.get("category");
        String subject  = body.get("subject");
        String message  = body.get("message");
        String priority = body.getOrDefault("priority", "MEDIUM");

        if (subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subject is required");
        }
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message is required");
        }

        DomainEnums.SupportTicketPriority ticketPriority;
        try {
            ticketPriority = DomainEnums.SupportTicketPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            ticketPriority = DomainEnums.SupportTicketPriority.MEDIUM;
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setId(UUID.randomUUID().toString());
        ticket.setUserId(userId);
        ticket.setCategory(category != null ? category.toUpperCase() : "OTHER");
        ticket.setSubject(subject.trim());
        ticket.setMessage(message.trim());
        ticket.setPriority(ticketPriority);
        ticket.setStatus(DomainEnums.SupportTicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        return supportTicketRepository.save(ticket);
    }

    @GetMapping("/admin/tickets")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('SUPPORT')")
    public List<SupportTicket> listAllAdminTickets() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/admin/tickets/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('SUPPORT')")
    public SupportTicket getAdminTicket(@PathVariable("id") String id) {
        return supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
    }

    @PostMapping("/admin/tickets/{id}/assign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('SUPPORT')")
    public SupportTicket assignTicket(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ticket.setAssignedAdminId(body.getOrDefault("assignedAdminId", currentUserService.requireCurrentUser().getId()));
        ticket.setStatus(DomainEnums.SupportTicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        return supportTicketRepository.save(ticket);
    }

    @PostMapping("/admin/tickets/{id}/respond")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('SUPPORT')")
    public SupportTicket respondTicket(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        String response = body.get("response");
        if (response == null || response.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "response text is required");
        }
        ticket.setAdminResponse(response.trim());
        ticket.setRespondedAt(LocalDateTime.now());
        ticket.setRespondedByAdminId(currentUserService.requireCurrentUser().getId());
        ticket.setStatus(DomainEnums.SupportTicketStatus.RESOLVED);
        ticket.setUpdatedAt(LocalDateTime.now());
        return supportTicketRepository.save(ticket);
    }

    @PostMapping("/admin/tickets/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('SUPPORT')")
    public SupportTicket updateTicketStatus(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        String status = body.get("status");
        if (status != null) {
            ticket.setStatus(DomainEnums.SupportTicketStatus.valueOf(status.toUpperCase()));
            ticket.setUpdatedAt(LocalDateTime.now());
        }
        return supportTicketRepository.save(ticket);
    }
}
