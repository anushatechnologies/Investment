package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.enabled:false}") boolean enabled,
                        @Value("${app.email.from:}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean sendSignupOtp(String email, String otp) {
        return send(email, "Your AnushaTrade signup OTP",
                "Your signup OTP is " + otp + ". It expires in 10 minutes.");
    }

    public boolean sendPasswordReset(String email, String resetLink, String token) {
        return send(email, "Reset your AnushaTrade password",
                "Use this link to reset your password:\n\n" + resetLink
                        + "\n\nIf your app asks for a token, use:\n" + token
                        + "\n\nThis reset token expires in 24 hours.");
    }

    private boolean send(String to, String subject, String body) {
        if (!enabled) {
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            System.err.println("[EmailService] Email delivery failed: " + ex.getMessage());
            return false;
        }
    }
}
