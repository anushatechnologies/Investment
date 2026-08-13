package com.anushabazaar.backend.service;

import com.anushabazaar.backend.config.RazorpayProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.security.MessageDigest;

@Service
public class RazorpayGatewayService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final RazorpayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public RazorpayGatewayService(RazorpayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public RazorpayProperties properties() {
        return properties;
    }

    public Map<String, Object> createOrder(BigDecimal amount, String currency, String receipt, Map<String, Object> notes) {
        ensureConfigured();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", toSubunits(amount));
        payload.put("currency", currency);
        payload.put("receipt", receipt);
        payload.put("notes", notes);
        return send("POST", "/orders", payload);
    }

    public Map<String, Object> fetchPayment(String paymentId) {
        ensureConfigured();
        return send("GET", "/payments/" + urlEncode(paymentId), null);
    }

    public Map<String, Object> capturePayment(String paymentId, BigDecimal amount, String currency) {
        ensureConfigured();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", toSubunits(amount));
        payload.put("currency", currency);
        return send("POST", "/payments/" + urlEncode(paymentId) + "/capture", payload);
    }

    public Map<String, Object> createRefund(String paymentId, BigDecimal amount, String notes) {
        ensureConfigured();
        Map<String, Object> payload = new LinkedHashMap<>();
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            payload.put("amount", toSubunits(amount));
        }
        if (notes != null && !notes.isBlank()) {
            Map<String, Object> notesMap = new LinkedHashMap<>();
            notesMap.put("reason", notes);
            payload.put("notes", notesMap);
        }
        return send("POST", "/payments/" + urlEncode(paymentId) + "/refund", payload);
    }

    public Map<String, Object> fetchSettlements(Integer count, Integer skip) {
        ensureConfigured();
        StringBuilder path = new StringBuilder("/settlements");
        if (count != null || skip != null) {
            path.append("?");
            if (count != null) {
                path.append("count=").append(count);
            }
            if (skip != null) {
                if (path.charAt(path.length() - 1) != '?') {
                    path.append("&");
                }
                path.append("skip=").append(skip);
            }
        }
        return send("GET", path.toString(), null);
    }

    public boolean verifyCheckoutSignature(String orderId, String paymentId, String signature) {
        ensureConfigured();
        String payload = orderId + "|" + paymentId;
        return secureEquals(hmacHex(payload, properties.getKeySecret()), signature);
    }

    public boolean verifyWebhookSignature(String rawPayload, String signature) {
        ensureConfigured();
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Razorpay webhook secret is not configured");
        }
        return secureEquals(hmacHex(rawPayload, properties.getWebhookSecret()), signature);
    }

    public String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize Razorpay payload");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> nestedMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Map<String, Object> send(String method, String path, Map<String, Object> payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getApiBaseUrl() + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", basicAuthHeader())
                    .header("Content-Type", "application/json");

            if ("GET".equals(method)) {
                builder.GET();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(payload == null ? "" : objectMapper.writeValueAsString(payload)));
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Razorpay API error: " + response.body());
            }
            return objectMapper.readValue(response.body(), MAP_TYPE);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not complete Razorpay API call");
        }
    }

    private void ensureConfigured() {
        if (!properties.isEnabled() || properties.getKeyId() == null || properties.getKeyId().isBlank()
                || properties.getKeySecret() == null || properties.getKeySecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay is not configured on this backend");
        }
    }

    private long toSubunits(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private String basicAuthHeader() {
        String token = properties.getKeyId() + ":" + properties.getKeySecret();
        return "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacHex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not verify Razorpay signature");
        }
    }

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
