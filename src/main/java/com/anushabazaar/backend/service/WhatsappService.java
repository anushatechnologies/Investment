package com.anushabazaar.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class WhatsappService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappService.class);
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${app.whatsapp.phone-number-id:mock_phone_number_id}")
    private String phoneNumberId;

    @Value("${app.whatsapp.access-token:mock_access_token}")
    private String accessToken;

    @Value("${app.whatsapp.enabled:true}")
    private boolean enabled;

    public WhatsappService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /** WhatsApp is retained for authentication OTPs only, never for payment receipts. */
    public boolean sendOtpWhatsapp(String mobileNumber, String otp, String purpose) {
        if (!enabled) {
            log.info("WhatsApp OTP disabled. OTP for {} ({}): {}", mobileNumber, purpose, otp);
            return false;
        }

        String recipientPhone = formatPhoneNumber(mobileNumber);
        if (recipientPhone.isBlank()) {
            log.warn("Cannot send OTP WhatsApp - invalid phone: {}", mobileNumber);
            return false;
        }

        try {
            if ("mock_access_token".equals(accessToken) || "mock_phone_number_id".equals(phoneNumberId)) {
                log.info("[SIMULATED] WhatsApp OTP {} for {} ({}): {}", purpose, recipientPhone, purpose, otp);
                return true;
            }

            String url = "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, Object> textPayload = new LinkedHashMap<>();
            textPayload.put("messaging_product", "whatsapp");
            textPayload.put("recipient_type", "individual");
            textPayload.put("to", recipientPhone);
            textPayload.put("type", "text");
            textPayload.put("text", Map.of(
                    "preview_url", false,
                    "body", String.format("Anusha Trade OTP\n\nYour %s verification code is:\n\n%s\n\nThis code is valid for 10 minutes. Do not share it with anyone.", purpose, otp)
            ));

            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(textPayload), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp OTP sent successfully to {}", recipientPhone);
                return true;
            }
            log.warn("WhatsApp OTP send failed for {} - HTTP {}", recipientPhone, response.getStatusCode().value());
            return false;
        } catch (Exception ex) {
            log.error("Error sending WhatsApp OTP to {}: {}", recipientPhone, ex.getMessage());
            return false;
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^\\d]", "");
        return cleaned.length() == 10 ? "91" + cleaned : cleaned;
    }
}
