package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.PaymentReceipt;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.repository.PaymentReceiptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Service
public class WhatsappService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappService.class);

    private final PaymentReceiptRepository receiptRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.whatsapp.phone-number-id:mock_phone_number_id}")
    private String phoneNumberId;

    @Value("${app.whatsapp.access-token:mock_access_token}")
    private String accessToken;

    @Value("${app.whatsapp.template-name:investment_payment_receipt}")
    private String templateName;

    @Value("${app.whatsapp.enabled:true}")
    private boolean enabled;

    public WhatsappService(PaymentReceiptRepository receiptRepository, ObjectMapper objectMapper) {
        this.receiptRepository = receiptRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Async
    public void sendReceiptWhatsapp(PaymentReceipt receipt, User user) {
        if (!enabled) {
            log.info("WhatsApp delivery disabled in configuration. Skipping for receipt {}", receipt.getReceiptNumber());
            return;
        }

        receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.SENDING);
        receiptRepository.save(receipt);

        String recipientPhone = formatPhoneNumber(receipt.getWhatsappPhoneNumber() != null ? receipt.getWhatsappPhoneNumber() : user.getMobileNumber());

        try {
            if ("mock_access_token".equals(accessToken) || "mock_phone_number_id".equals(phoneNumberId)) {
                log.info("Using simulated Meta WhatsApp dispatch for phone {}", recipientPhone);
                String simulatedWamid = "wamid.simulated_" + UUID.randomUUID().toString().replace("-", "");
                receipt.setWhatsappWamid(simulatedWamid);
                receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.SENT);
                receiptRepository.save(receipt);
                return;
            }

            String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> payload = buildPayload(recipientPhone, user.getFullName(), receipt.getPaymentAmount(), receipt.getInvestmentId(), receipt.getReceiptNumber(), receipt.getReceiptUrl());

            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode messagesNode = root.path("messages");
                if (messagesNode.isArray() && !messagesNode.isEmpty()) {
                    String wamid = messagesNode.get(0).path("id").asText();
                    receipt.setWhatsappWamid(wamid);
                    receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.SENT);
                    log.info("Successfully dispatched WhatsApp receipt {} with wamid {}", receipt.getReceiptNumber(), wamid);
                } else {
                    receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.FAILED);
                    receipt.setWhatsappErrorReason("No message ID returned from Meta API");
                }
            } else {
                receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.FAILED);
                receipt.setWhatsappErrorReason("Meta API returned HTTP " + response.getStatusCode().value());
            }
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message for receipt " + receipt.getReceiptNumber(), ex);
            receipt.setWhatsappStatus(DomainEnums.WhatsappStatus.FAILED);
            receipt.setWhatsappErrorReason(ex.getMessage());
        } finally {
            receiptRepository.save(receipt);
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^\\d]", "");
        if (cleaned.length() == 10) {
            return "91" + cleaned;
        }
        return cleaned;
    }

    private Map<String, Object> buildPayload(String recipientPhone, String userName, BigDecimal amount, String investmentId, String receiptNumber, String receiptUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", recipientPhone);
        payload.put("type", "template");

        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", templateName);

        Map<String, Object> language = new LinkedHashMap<>();
        language.put("code", "en");
        template.put("language", language);

        List<Map<String, Object>> components = new ArrayList<>();

        Map<String, Object> headerComponent = new LinkedHashMap<>();
        headerComponent.put("type", "header");
        List<Map<String, Object>> headerParams = new ArrayList<>();
        Map<String, Object> documentParam = new LinkedHashMap<>();
        documentParam.put("type", "document");

        Map<String, Object> documentDetails = new LinkedHashMap<>();
        documentDetails.put("link", receiptUrl != null ? receiptUrl : "https://storage.anusha.trade/receipts/" + receiptNumber + ".pdf");
        documentDetails.put("filename", "Anusha_Trade_Receipt_" + receiptNumber + ".pdf");
        documentParam.put("document", documentDetails);
        headerParams.add(documentParam);
        headerComponent.put("parameters", headerParams);
        components.add(headerComponent);

        Map<String, Object> bodyComponent = new LinkedHashMap<>();
        bodyComponent.put("type", "body");
        List<Map<String, Object>> bodyParams = new ArrayList<>();

        bodyParams.add(Map.of("type", "text", "text", userName != null ? userName : "Investor"));
        bodyParams.add(Map.of("type", "text", "text", "₹" + NumberFormat.getInstance(new Locale("en", "IN")).format(amount)));
        bodyParams.add(Map.of("type", "text", "text", investmentId != null ? investmentId : ""));
        bodyParams.add(Map.of("type", "text", "text", receiptNumber != null ? receiptNumber : ""));

        bodyComponent.put("parameters", bodyParams);
        components.add(bodyComponent);

        template.put("components", components);
        payload.put("template", template);

        return payload;
    }
}
