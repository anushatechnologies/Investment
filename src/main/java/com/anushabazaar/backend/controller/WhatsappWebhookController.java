package com.anushabazaar.backend.controller;

import com.anushabazaar.backend.service.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final ReceiptService receiptService;

    @Value("${app.whatsapp.webhook-verify-token:anusha_whatsapp_verify_token}")
    private String verifyToken;

    public WhatsappWebhookController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        log.info("Received Meta WhatsApp Webhook verification request: mode={}, token={}", mode, token);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp Webhook verification successful");
            return ResponseEntity.ok(challenge != null ? challenge : "");
        } else {
            log.warn("WhatsApp Webhook verification failed. Token mismatch.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
        }
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhookEvent(@RequestBody String payload) {
        log.debug("Received Meta WhatsApp Webhook event payload: {}", payload);
        receiptService.processWebhookPayload(payload);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
