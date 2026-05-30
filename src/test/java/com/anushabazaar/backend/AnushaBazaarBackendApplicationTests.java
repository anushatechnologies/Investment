package com.anushabazaar.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnushaBazaarBackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void corsPreflightAllowsProductionFrontend() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://anushatrade.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://anushatrade.com"));
    }

    @Test
    void mobileOtpCanBeGeneratedForSignup() throws Exception {
        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "mobileNumber", "9948598350",
                                "channel", "MOBILE_OTP",
                                "useFirebase", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("MOBILE_OTP"))
                .andExpect(jsonPath("$.otp").exists());
    }

    @Test
    void mobileOtpCanBeGeneratedForForgotPasswordExistingUser() throws Exception {
        registerVerifyAndLogin("forgot-mobile@example.com", "9948598351");

        JsonNode sendOtp = postJsonWithoutAuth("/api/auth/send-otp", Map.of(
                "mobileNumber", "+919948598351",
                "channel", "MOBILE_OTP",
                "type", "FORGOT_PASSWORD"
        ));
        JsonNode verifyOtp = postJsonWithoutAuth("/api/auth/verify-otp", Map.of(
                "mobileNumber", "+919948598351",
                "otp", sendOtp.get("otp").asText(),
                "type", "FORGOT_PASSWORD"
        ));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", verifyOtp.get("resetToken").asText(), "newPassword", "Investor@456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }

    @Test
    void fileViewServesUploadedFileInline() throws Exception {
        Path upload = Path.of("target/investment-test-uploads/kyc/test-image.png");
        Files.createDirectories(upload.getParent());
        Files.write(upload, "fake-image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/files/view")
                        .queryParam("path", "kyc/test-image.png")
                        .header("Origin", "http://anushatrade.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://anushatrade.com"));
    }

    @Test
    void publicEndpointsAndRoleProtectionWork() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").exists());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());

        String investorToken = createVerifiedInvestorAndLogin("security-check@example.com", "9876500001");
        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void investorAndAdminApiFlowWorks() throws Exception {
        String adminToken = login("admin@anushabazaar.com", "Admin@123").get("accessToken").asText();
        JsonNode investorLogin = registerVerifyAndLogin("flow-investor@example.com", "9876500002");
        String investorToken = investorLogin.get("accessToken").asText();
        String investorUserId = investorLogin.get("userId").asText();

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", investorLogin.get("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        String kycId = submitKyc(investorToken)
                .get("id").asText();

        mockMvc.perform(get("/api/kyc/status").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("PENDING"));

        mockMvc.perform(get("/api/admin/kyc/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/kyc/{id}/documents", kycId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panCard").exists())
                .andExpect(jsonPath("$.bankProof").exists())
                .andExpect(jsonPath("$.panCardStatus").value("PENDING"));

        mockMvc.perform(post("/api/admin/kyc/{id}/documents/reject", kycId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "reason", "Please reupload PAN card",
                                "adminNotes", "PAN blur",
                                "panCard", true,
                                "aadhaarFront", false,
                                "aadhaarBack", false,
                                "selfie", false,
                                "bankProof", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REUPLOAD_REQUIRED"))
                .andExpect(jsonPath("$.panCardStatus").value("REUPLOAD_REQUIRED"));

        mockMvc.perform(get("/api/kyc/status").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kycStatus").value("REUPLOAD_REQUIRED"))
                .andExpect(jsonPath("$.submission.panCardStatus").value("REUPLOAD_REQUIRED"))
                .andExpect(jsonPath("$.submission.panCardRejectionReason").value("Please reupload PAN card"));

        mockMvc.perform(multipart("/api/kyc/submit")
                        .file(file("panCardImage", "pan-reupload.jpg"))
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarLast4", "1234")
                        .param("dateOfBirth", "1995-01-01")
                        .param("address", "Hyderabad")
                        .header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.panCardStatus").value("PENDING"))
                .andExpect(jsonPath("$.aadhaarFrontStatus").value("PENDING"));

        mockMvc.perform(post("/api/admin/kyc/{id}/approve", kycId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNotes", "verified in integration test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/bank/link")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "accountHolderName", "Integration Investor",
                                "bankAccountNumber", "1234567890",
                                "confirmBankAccountNumber", "1234567890",
                                "bankIfscCode", "SBIN0001234",
                                "bankName", "State Bank of India"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankVerified").value(true));

        mockMvc.perform(get("/api/bank/details").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankVerified").value(true));

        mockMvc.perform(post("/api/auth/activate")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("SET_MPIN"));

        mockMvc.perform(post("/api/auth/set-mpin")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mpin", "135790"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("OPEN_DASHBOARD"));

        mockMvc.perform(post("/api/auth/verify-mpin")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("mpin", "135790"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        JsonNode plan = firstNode(getJson("/api/plans", investorToken));
        String planId = plan.get("id").asText();

        mockMvc.perform(get("/api/admin/plans").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        String createdPlanId = postJson("/api/admin/plans", adminToken, Map.of(
                "planName", "Test Growth Plan",
                "description", "Created by API integration test",
                "minimumAmount", 5000,
                "maximumAmount", 250000,
                "lockInMonths", 12,
                "monthlyInterestRate", 1.8
        )).get("id").asText();

        mockMvc.perform(put("/api/admin/plans/{id}", createdPlanId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "planName", "Test Growth Plan Updated",
                                "description", "Updated by API integration test",
                                "minimumAmount", 5000,
                                "maximumAmount", 300000,
                                "lockInMonths", 12,
                                "monthlyInterestRate", 1.9,
                                "active", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Test Growth Plan Updated"));

        mockMvc.perform(post("/api/admin/plans/{id}/deactivate", createdPlanId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        String investmentId = postJson("/api/investments/apply", investorToken, Map.of(
                "investmentPlanId", planId,
                "investmentAmount", 100000
        )).get("id").asText();

        String cancellableInvestmentId = postJson("/api/investments/apply", investorToken, Map.of(
                "investmentPlanId", planId,
                "investmentAmount", 5000
        )).get("id").asText();

        mockMvc.perform(post("/api/investments/{id}/cancel", cancellableInvestmentId)
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "covered by integration test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        uploadReceipt(investorToken, investmentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationStatus").value("PENDING"));

        mockMvc.perform(get("/api/investments").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/investments/{id}", investmentId).header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(investmentId));

        mockMvc.perform(get("/api/admin/investments/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/investments").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/admin/investments/{id}/verify-receipt", investmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("approved", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receipt.verificationStatus").value("APPROVED"));

        mockMvc.perform(post("/api/admin/investments/{id}/activate", investmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("notes", "activated in integration test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/admin/interest/trigger").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Interest run completed"));

        mockMvc.perform(get("/api/wallet").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wallet").exists());

        mockMvc.perform(get("/api/wallet/transactions").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        String withdrawalId = postJson("/api/withdrawals/request", investorToken, Map.of("requestedAmount", 1000))
                .get("id").asText();

        mockMvc.perform(get("/api/withdrawals").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/withdrawals/pending").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/admin/withdrawals/{id}/approve", withdrawalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("adminNotes", "approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/admin/withdrawals/{id}/process", withdrawalId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bankTransferReference", "UTR123456", "adminNotes", "processed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        mockMvc.perform(get("/api/referrals/tree").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tree").exists());

        mockMvc.perform(get("/api/referrals/commissions").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/summary").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadNotifications").exists());

        JsonNode notifications = getJson("/api/notifications", investorToken);
        if (!notifications.isEmpty()) {
            String notificationId = notifications.get(0).get("id").asText();

            mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                            .header("Authorization", "Bearer " + investorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.readFlag").value(true));

            mockMvc.perform(post("/api/notifications/read-all")
                            .header("Authorization", "Bearer " + investorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updatedCount").exists());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/notifications/{id}", notificationId)
                            .header("Authorization", "Bearer " + investorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(notificationId));
        }

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.email").value("flow-investor@example.com"));

        mockMvc.perform(get("/api/admin/interest/rates").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(put("/api/admin/interest/rates")
                        .queryParam("planId", planId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("monthlyInterestRate", 1.6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyInterestRate").value(1.6));

        mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvestors", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(put("/api/admin/users/{id}", investorUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + investorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "Investor@123", "newPassword", "Investor@321"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        mockMvc.perform(post("/api/admin/users/{id}/suspend", investorUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("reason", "covered by integration test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("SUSPENDED"));

        mockMvc.perform(put("/api/admin/users/{id}", investorUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        JsonNode fraudAlerts = getJson("/api/admin/fraud-alerts", adminToken);
        if (!fraudAlerts.isEmpty()) {
            mockMvc.perform(post("/api/admin/fraud-alerts/{id}/resolve", fraudAlerts.get(0).get("id").asText())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("resolutionNotes", "resolved by integration test", "status", "RESOLVED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RESOLVED"));
        }

        mockMvc.perform(get("/api/admin/audit-logs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/admin/reports/monthly").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").exists());

        JsonNode forgotPassword = postJsonWithoutAuth("/api/auth/forgot-password", Map.of("email", "flow-investor@example.com"));
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", forgotPassword.get("resetToken").asText(), "newPassword", "Investor@456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + investorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    private String createVerifiedInvestorAndLogin(String email, String mobileNumber) throws Exception {
        return registerVerifyAndLogin(email, mobileNumber).get("accessToken").asText();
    }

    private JsonNode registerVerifyAndLogin(String email, String mobileNumber) throws Exception {
        JsonNode sendOtp = postJsonWithoutAuth("/api/auth/send-otp", Map.of("email", email));
        JsonNode verifyOtp = postJsonWithoutAuth("/api/auth/verify-otp", Map.of(
                "email", email,
                "otp", sendOtp.get("otp").asText()
        ));
        assertThat(verifyOtp.get("signupVerificationExpiresInMinutes").asInt()).isEqualTo(60);
        Map<String, Object> body = registrationBody(email, mobileNumber);
        body.put("signupVerificationToken", verifyOtp.get("signupVerificationToken").asText());
        JsonNode register = postJsonWithoutAuth("/api/auth/register", body);

        mockMvc.perform(get("/api/auth/verify-email")
                        .queryParam("token", register.get("verificationToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));

        return login(email, "Investor@123");
    }

    private JsonNode login(String email, String password) throws Exception {
        return postJsonWithoutAuth("/api/auth/login", Map.of("email", email, "password", password));
    }

    private Map<String, Object> registrationBody(String email, String mobileNumber) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fullName", "Integration Investor");
        body.put("email", email);
        body.put("mobileNumber", mobileNumber);
        body.put("password", "Investor@123");
        body.put("riskDisclosureAccepted", true);
        body.put("investorAgreementAccepted", true);
        return body;
    }

    private JsonNode submitKyc(String token) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/kyc/submit")
                        .file(file("panCardImage", "pan.jpg"))
                        .file(file("aadhaarFrontImage", "aadhaar-front.jpg"))
                        .file(file("aadhaarBackImage", "aadhaar-back.jpg"))
                        .file(file("selfiePhoto", "selfie.jpg"))
                        .file(file("bankPassbookOrStatement", "bank.jpg"))
                        .param("panNumber", "ABCDE1234F")
                        .param("aadhaarLast4", "1234")
                        .param("dateOfBirth", "1995-01-01")
                        .param("address", "Hyderabad")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private org.springframework.test.web.servlet.ResultActions uploadReceipt(String token, String investmentId) throws Exception {
        return mockMvc.perform(multipart("/api/investments/{id}/upload-receipt", investmentId)
                .file(file("receiptFile", "receipt.jpg"))
                .param("paymentAmount", "100000")
                .param("paymentDate", LocalDate.now().toString())
                .param("paymentMode", "NEFT")
                .param("bankReference", "BANKREF123")
                .header("Authorization", "Bearer " + token));
    }

    private JsonNode postJson(String path, String token, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private JsonNode postJsonWithoutAuth(String path, Object body) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return read(result);
    }

    private JsonNode firstNode(JsonNode array) {
        if (array.isEmpty()) {
            throw new AssertionError("Expected response array to contain at least one item");
        }
        return array.get(0);
    }

    private MockMultipartFile file(String fieldName, String filename) {
        return new MockMultipartFile(
                fieldName,
                filename,
                MediaType.IMAGE_JPEG_VALUE,
                ("test-file-" + filename).getBytes(StandardCharsets.UTF_8)
        );
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}


