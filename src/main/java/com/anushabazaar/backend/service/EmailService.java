package com.anushabazaar.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

    public void sendSignupOtp(String email, String otp) {
        send(email, "Your AnushaTrade signup OTP",
                "Your signup OTP is " + otp + ". It expires in 10 minutes.");
    }

    public void sendPasswordReset(String email, String resetLink, String token) {
        send(email, "Reset your AnushaTrade password",
                "Use this link to reset your password:\n\n" + resetLink
                        + "\n\nIf your app asks for a token, use:\n" + token
                        + "\n\nThis reset token expires in 24 hours.");
    }

    private void send(String to, String subject, String body) {
        if (!enabled) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
