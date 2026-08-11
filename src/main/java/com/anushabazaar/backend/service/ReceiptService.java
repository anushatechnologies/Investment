package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.Investment;
import com.anushabazaar.backend.domain.PaymentReceipt;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.repository.InvestmentRepository;
import com.anushabazaar.backend.repository.PaymentReceiptRepository;
import com.anushabazaar.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private final PaymentReceiptRepository receiptRepository;
    private final InvestmentRepository investmentRepository;
    private final UserRepository userRepository;
    private final WhatsappService whatsappService;
    private final ObjectMapper objectMapper;

    public ReceiptService(PaymentReceiptRepository receiptRepository,
                          InvestmentRepository investmentRepository,
                          UserRepository userRepository,
                          WhatsappService whatsappService,
                          ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.investmentRepository = investmentRepository;
        this.userRepository = userRepository;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
    }

    public ApiDtos.ReceiptStatusResponse getReceiptStatus(User user, String investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investment not found"));

        if (!user.getRole().name().contains("ADMIN") && !investment.getInvestorUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to investment receipt");
        }

        PaymentReceipt receipt = receiptRepository.findTopByInvestmentIdOrderByUploadedAtDesc(investmentId)
                .orElse(null);

        String paymentStatus = mapInvestmentToPaymentStatus(investment.getStatus());

        if (receipt == null) {
            return new ApiDtos.ReceiptStatusResponse(
                    paymentStatus,
                    investment.getId(),
                    investment.getInvestmentAmount(),
                    new ApiDtos.ReceiptStatusDetails(
                            null, null,
                            DomainEnums.EmailStatus.NOT_SENT.name(),
                            DomainEnums.WhatsappStatus.NOT_SENT.name(),
                            false
                    )
            );
        }

        ensureReceiptNumberAndUrl(receipt);

        String emailStatusStr = receipt.getEmailStatus() != null ? receipt.getEmailStatus().name() : DomainEnums.EmailStatus.NOT_SENT.name();
        String whatsappStatusStr = receipt.getWhatsappStatus() != null ? receipt.getWhatsappStatus().name() : DomainEnums.WhatsappStatus.NOT_SENT.name();

        return new ApiDtos.ReceiptStatusResponse(
                paymentStatus,
                investment.getId(),
                investment.getInvestmentAmount(),
                new ApiDtos.ReceiptStatusDetails(
                        receipt.getReceiptNumber(),
                        receipt.getReceiptUrl(),
                        emailStatusStr,
                        whatsappStatusStr,
                        true
                )
        );
    }

    public void triggerReceiptDelivery(PaymentReceipt receipt, User user) {
        ensureReceiptNumberAndUrl(receipt);

        if (receipt.getWhatsappStatus() == null || receipt.getWhatsappStatus() == DomainEnums.WhatsappStatus.NOT_SENT) {
            receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.QUEUED);
        }
        if (receipt.getEmailStatus() == null || receipt.getEmailStatus() == DomainEnums.EmailStatus.NOT_SENT) {
            receipt.setEmailStatus(DomainEnums.EmailStatus.SENT);
        }
        if (receipt.getWhatsappPhoneNumber() == null) {
            receipt.setWhatsappPhoneNumber(user.getMobileNumber());
        }

        receiptRepository.save(receipt);

        whatsappService.sendReceiptWhatsapp(receipt, user);
    }

    public void processWebhookPayload(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String objectType = root.path("object").asText();

            if ("whatsapp_business_account".equals(objectType)) {
                JsonNode entries = root.path("entry");
                if (entries.isArray()) {
                    for (JsonNode entry : entries) {
                        JsonNode changes = entry.path("changes");
                        if (changes.isArray()) {
                            for (JsonNode change : changes) {
                                JsonNode value = change.path("value");
                                JsonNode statuses = value.path("statuses");
                                if (statuses.isArray()) {
                                    for (JsonNode statusNode : statuses) {
                                        updateStatusFromWebhook(statusNode);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse Meta WhatsApp Webhook payload", ex);
        }
    }

    private void updateStatusFromWebhook(JsonNode statusNode) {
        String wamid = statusNode.path("id").asText();
        String statusType = statusNode.path("status").asText();

        if (wamid == null || wamid.isBlank()) return;

        Optional<PaymentReceipt> receiptOpt = receiptRepository.findByWhatsappWamid(wamid);
        if (receiptOpt.isEmpty()) {
            log.warn("Received WhatsApp status update for unknown wamid: {}", wamid);
            return;
        }

        PaymentReceipt receipt = receiptOpt.get();
        DomainEnums.WhatsappStatus mappedStatus;

        switch (statusType.toLowerCase()) {
            case "sent":
                mappedStatus = DomainEnums.WhatsappStatus.SENT;
                break;
            case "delivered":
                mappedStatus = DomainEnums.WhatsappStatus.DELIVERED;
                break;
            case "read":
                mappedStatus = DomainEnums.WhatsappStatus.READ;
                break;
            case "failed":
                mappedStatus = DomainEnums.WhatsappStatus.FAILED;
                if (statusNode.has("errors")) {
                    receipt.setWhatsappErrorReason(statusNode.path("errors").toString());
                }
                break;
            default:
                mappedStatus = DomainEnums.WhatsappStatus.SENT;
        }

        receipt.setWhatsappStatus(mappedStatus);
        receiptRepository.save(receipt);
        log.info("Updated WhatsApp status for receipt {} (wamid {}) to {}", receipt.getReceiptNumber(), wamid, mappedStatus);
    }

    private String mapInvestmentToPaymentStatus(DomainEnums.InvestmentStatus status) {
        if (status == DomainEnums.InvestmentStatus.ACTIVE || status == DomainEnums.InvestmentStatus.MATURED) {
            return "SUCCESS";
        } else if (status == DomainEnums.InvestmentStatus.REJECTED || status == DomainEnums.InvestmentStatus.CANCELLED) {
            return "FAILED";
        } else {
            return "PENDING";
        }
    }

    private void ensureReceiptNumberAndUrl(PaymentReceipt receipt) {
        boolean updated = false;
        if (receipt.getReceiptNumber() == null || receipt.getReceiptNumber().isBlank()) {
            receipt.setReceiptNumber("ATR-2026-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
            updated = true;
        }
        if (receipt.getReceiptUrl() == null || receipt.getReceiptUrl().isBlank()) {
            receipt.setReceiptUrl("https://storage.anusha.trade/receipts/" + receipt.getReceiptNumber() + ".pdf");
            updated = true;
        }
        if (updated) {
            receiptRepository.save(receipt);
        }
    }
}
