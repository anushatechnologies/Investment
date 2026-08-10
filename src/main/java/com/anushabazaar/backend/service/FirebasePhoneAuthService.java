package com.anushabazaar.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class FirebasePhoneAuthService {

    private static final String APP_NAME = "anushatrade";

    private final ResourceLoader resourceLoader;
    private final String projectId;
    private final String serviceAccountResource;
    private final String serviceAccountPath;
    private final String serviceAccountJson;
    private final String serviceAccountBase64;

    private FirebaseAuth firebaseAuth;

    public FirebasePhoneAuthService(ResourceLoader resourceLoader,
                                    @Value("${app.firebase.project-id:}") String projectId,
                                    @Value("${app.firebase.service-account-resource:}") String serviceAccountResource,
                                    @Value("${app.firebase.service-account-path:}") String serviceAccountPath,
                                    @Value("${app.firebase.service-account-json:}") String serviceAccountJson,
                                    @Value("${app.firebase.service-account-base64:}") String serviceAccountBase64) {
        this.resourceLoader = resourceLoader;
        this.projectId = projectId;
        this.serviceAccountResource = serviceAccountResource;
        this.serviceAccountPath = serviceAccountPath;
        this.serviceAccountJson = serviceAccountJson;
        this.serviceAccountBase64 = serviceAccountBase64;
    }

    public VerifiedFirebasePhone verifyPhoneToken(String idToken) {
        try {
            FirebaseToken token = getFirebaseAuth().verifyIdToken(idToken);
            Object phoneClaim = token.getClaims().get("phone_number");
            if (phoneClaim == null || phoneClaim.toString().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase token does not contain a verified phone number");
            }
            return new VerifiedFirebasePhone(token.getUid(), normalizeMobile(phoneClaim.toString()));
        } catch (FirebaseAuthException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Firebase OTP token");
        }
    }

    private synchronized FirebaseAuth getFirebaseAuth() {
        if (firebaseAuth != null) {
            return firebaseAuth;
        }
        try {
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(existing -> APP_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(this::initializeFirebaseApp);
            firebaseAuth = FirebaseAuth.getInstance(app);
            return firebaseAuth;
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase is not configured");
        }
    }

    private FirebaseApp initializeFirebaseApp() {
        try (InputStream credentialsStream = credentialsStream()) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream));
            if (hasText(projectId)) {
                builder.setProjectId(projectId);
            }
            return FirebaseApp.initializeApp(builder.build(), APP_NAME);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase credentials could not be loaded");
        }
    }

    private InputStream credentialsStream() throws IOException {
        if (hasText(serviceAccountJson)) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }
        if (hasText(serviceAccountBase64)) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(serviceAccountBase64));
        }
        if (hasText(serviceAccountResource)) {
            Resource resource = resourceLoader.getResource(toResourceLocation(serviceAccountResource));
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase service account resource not found");
            }
            return resource.getInputStream();
        }
        if (hasText(serviceAccountPath)) {
            return new FileInputStream(serviceAccountPath);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase service account is missing");
    }

    private String toResourceLocation(String configuredResource) {
        if (configuredResource.startsWith("classpath:") || configuredResource.startsWith("file:")) {
            return configuredResource;
        }
        return "classpath:" + configuredResource;
    }

    private String normalizeMobile(String firebasePhoneNumber) {
        String digits = firebasePhoneNumber.replaceAll("\\D", "");
        if (digits.length() < 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Firebase phone number is invalid");
        }
        return digits.substring(digits.length() - 10);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record VerifiedFirebasePhone(String firebaseUid, String mobileNumber) {
    }
}
