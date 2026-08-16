package com.anushabazaar.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final RestTemplate restTemplate;

    @Value("${app.sms.fast2sms-api-key:}")
    private String fast2smsApiKey;

    @Value("${app.sms.enabled:true}")
    private boolean enabled;

    public SmsService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return fast2smsApiKey != null && !fast2smsApiKey.isBlank() && !fast2smsApiKey.equalsIgnoreCase("mock_api_key");
    }

    /**
     * Dispatches a real 6-digit OTP SMS to Indian 10-digit mobile number.
     */
    public boolean sendOtpSms(String mobileNumber, String otp, String purpose) {
        if (!enabled) {
            log.info("[SMS DISABLED] OTP for {}: {}", mobileNumber, otp);
            return false;
        }

        String digitsOnly = mobileNumber != null ? mobileNumber.replaceAll("\\D", "") : "";
        if (digitsOnly.length() > 10) {
            digitsOnly = digitsOnly.substring(digitsOnly.length() - 10);
        }

        if (digitsOnly.length() != 10) {
            log.warn("[SmsService] Invalid mobile number for SMS delivery: {}", mobileNumber);
            return false;
        }

        // 1. Fast2SMS Indian Gateway (Instant OTP Route)
        if (isConfigured()) {
            try {
                String url = "https://www.fast2sms.com/dev/bulkV2?authorization=" + URLEncoder.encode(fast2smsApiKey, StandardCharsets.UTF_8)
                        + "&variables_values=" + URLEncoder.encode(otp, StandardCharsets.UTF_8)
                        + "&route=otp&numbers=" + digitsOnly;

                HttpHeaders headers = new HttpHeaders();
                headers.set("cache-control", "no-cache");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("[SmsService] Successfully dispatched Fast2SMS OTP to +91 {}", digitsOnly);
                    return true;
                } else {
                    log.warn("[SmsService] Fast2SMS returned status {}: {}", response.getStatusCode(), response.getBody());
                }
            } catch (Exception ex) {
                log.error("[SmsService] Error sending Fast2SMS OTP to +91 {}: {}", digitsOnly, ex.getMessage());
            }
        } else {
            log.info("[SmsService - SIMULATED] Fast2SMS API Key not configured. Simulated SMS OTP for +91 {}: {}", digitsOnly, otp);
        }

        return false;
    }
}
