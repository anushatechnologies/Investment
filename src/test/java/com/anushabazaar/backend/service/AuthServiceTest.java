package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.TokenRecord;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.domain.Wallet;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.dto.FirebaseLoginRequest;
import com.anushabazaar.backend.repository.ReferralRelationshipRepository;
import com.anushabazaar.backend.repository.TokenRecordRepository;
import com.anushabazaar.backend.repository.UserRepository;
import com.anushabazaar.backend.repository.WalletRepository;
import com.anushabazaar.backend.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private ReferralRelationshipRepository referralRelationshipRepository;
    @Mock
    private TokenRecordRepository tokenRecordRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailService emailService;
    @Mock
    private WhatsappService whatsappService;
    @Mock
    private SmsService smsService;
    @Mock
    private FirebasePhoneAuthService firebasePhoneAuthService;
    @Mock
    private HttpServletRequest servletRequest;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                walletRepository,
                referralRelationshipRepository,
                tokenRecordRepository,
                passwordEncoder,
                jwtService,
                auditService,
                emailService,
                whatsappService,
                smsService,
                firebasePhoneAuthService,
                30
        );
    }

    @Test
    void testSendOtp_GeneratesAndStoresOtp() {
        ApiDtos.SendOtpRequest request = new ApiDtos.SendOtpRequest(null, "9876543210", null, null, null);
        when(userRepository.findByMobileNumberEndingWith("9876543210")).thenReturn(Optional.empty());
        when(smsService.sendOtpSms(anyString(), anyString(), anyString())).thenReturn(true);
        when(whatsappService.sendOtpWhatsapp(anyString(), anyString(), anyString())).thenReturn(true);

        Map<String, Object> response = authService.sendOtp(request, servletRequest);

        assertNotNull(response);
        assertEquals("SUCCESS", response.get("status"));
        assertNotNull(response.get("otp"));
        assertEquals("9876543210", response.get("recipient"));
    }

    @Test
    void testVerifyOtp_WithBypassCode() {
        User user = new User();
        user.setId("user-123");
        user.setFullName("Test User");
        user.setMobileNumber("9876543210");
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);

        when(userRepository.findByMobileNumberEndingWith("9876543210")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(tokenRecordRepository.save(any(TokenRecord.class))).thenAnswer(i -> i.getArgument(0));

        ApiDtos.VerifyOtpRequest request = new ApiDtos.VerifyOtpRequest("9876543210", "123456");
        Map<String, Object> response = authService.verifyOtp(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.get("status"));
        assertTrue((Boolean) response.get("verified"));
        assertEquals("DASHBOARD", response.get("nextStep"));
        assertEquals("mock-access-token", response.get("accessToken"));
    }

    @Test
    void testVerifyOtp_WithGeneratedOtp() {
        ApiDtos.SendOtpRequest sendReq = new ApiDtos.SendOtpRequest(null, "9876543210", null, null, null);
        when(userRepository.findByMobileNumberEndingWith("9876543210")).thenReturn(Optional.empty());
        Map<String, Object> sendRes = authService.sendOtp(sendReq, servletRequest);
        String generatedOtp = (String) sendRes.get("otp");

        ApiDtos.VerifyOtpRequest verifyReq = new ApiDtos.VerifyOtpRequest("9876543210", generatedOtp);
        Map<String, Object> verifyRes = authService.verifyOtp(verifyReq);

        assertNotNull(verifyRes);
        assertEquals("SUCCESS", verifyRes.get("status"));
        assertTrue((Boolean) verifyRes.get("verified"));
        assertEquals("COMPLETE_PROFILE", verifyRes.get("nextStep"));
    }

    @Test
    void testFirebaseMobileLogin_ExistingUser() {
        when(firebasePhoneAuthService.verifyPhoneToken("mock-firebase-token"))
                .thenReturn(new FirebasePhoneAuthService.VerifiedFirebasePhone("fb-uid-1", "9876543210"));

        User user = new User();
        user.setId("user-456");
        user.setFullName("Firebase User");
        user.setMobileNumber("9876543210");
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);

        when(userRepository.findByMobileNumberEndingWith("9876543210")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("firebase-jwt-access-token");
        when(tokenRecordRepository.save(any(TokenRecord.class))).thenAnswer(i -> i.getArgument(0));

        FirebaseLoginRequest request = new FirebaseLoginRequest("mock-firebase-token", "9876543210");
        ResponseEntity<Map<String, Object>> response = authService.firebaseMobileLogin(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("DASHBOARD", body.get("nextStep"));
        assertEquals("firebase-jwt-access-token", body.get("accessToken"));
    }

    @Test
    void testFirebaseMobileLogin_NewUser() {
        when(firebasePhoneAuthService.verifyPhoneToken("mock-new-token"))
                .thenReturn(new FirebasePhoneAuthService.VerifiedFirebasePhone("fb-uid-2", "9123456789"));

        when(userRepository.findByMobileNumberEndingWith("9123456789")).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber("9123456789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("9123456789")).thenReturn(Optional.empty());

        FirebaseLoginRequest request = new FirebaseLoginRequest("mock-new-token", "9123456789");
        ResponseEntity<Map<String, Object>> response = authService.firebaseMobileLogin(request);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("COMPLETE_PROFILE", body.get("nextStep"));
        assertNotNull(body.get("signupVerificationToken"));
    }

    @Test
    void testLogin_WithPassword() {
        User user = new User();
        user.setId("user-789");
        user.setEmail("test@anusha.trade");
        user.setPasswordHash("$2a$10$encodedPassword");
        user.setRole(DomainEnums.Role.INVESTOR);
        user.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);

        when(userRepository.findByEmail("test@anusha.trade")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "$2a$10$encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("login-jwt-token");
        when(tokenRecordRepository.save(any(TokenRecord.class))).thenAnswer(i -> i.getArgument(0));

        ApiDtos.LoginRequest request = new ApiDtos.LoginRequest("test@anusha.trade", null, null, null, "Password@123", null);
        Map<String, Object> response = authService.login(request, servletRequest);

        assertNotNull(response);
        assertEquals("Login successful", response.get("message"));
        assertEquals("login-jwt-token", response.get("accessToken"));
        assertNotNull(response.get("refreshToken"));
    }

    @Test
    void testRegister_NewUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByMobileNumber(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("reg-jwt-token");
        when(tokenRecordRepository.save(any(TokenRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        ApiDtos.RegisterRequest request = new ApiDtos.RegisterRequest(
                "Ramesh Kumar", null, "ramesh@example.com", "9876500000", null,
                "Password@123", "1234", "1995-01-01", null, "ABCDE1234F", null,
                "1234", null, "123 Street", "1234567890", null, "HDFC0001234", null,
                "HDFC Bank", null, null, true, true, true, false
        );

        Map<String, Object> response = authService.register(request, servletRequest);

        assertNotNull(response);
        assertEquals("SUCCESS", response.get("status"));
        assertEquals("Registration successful.", response.get("message"));
        assertEquals("reg-jwt-token", response.get("accessToken"));
        assertNotNull(response.get("refreshToken"));
        assertNotNull(response.get("userId"));
    }
}
