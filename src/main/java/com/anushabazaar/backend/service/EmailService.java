package com.anushabazaar.backend.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.email.enabled:false}") boolean enabled,
                        @Value("${app.email.from:}") String from) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() {
        return enabled && mailSender != null;
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

    public boolean sendPaymentInvoice(String email, String investorName, String invoiceNumber,
                                      String amount, String paymentId, String invoiceUrl) {
        if (!isEnabled() || email == null || email.isBlank()) return false;
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            if (from != null && !from.isBlank()) helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("Payment successful - " + invoiceNumber + " | Anusha Trade");
            helper.setText("<div style='font-family:Arial,sans-serif;color:#10213a;max-width:640px;margin:auto'>"
                    + "<div style='background:#1261e8;color:white;padding:24px;border-radius:12px 12px 0 0'>"
                    + "<h1 style='margin:0'>ANUSHA TRADE</h1><p style='margin:8px 0 0'>Investment payment confirmation</p></div>"
                    + "<div style='padding:24px;border:1px solid #e5e7eb;border-top:0'>"
                    + "<h2>Payment successful</h2><p>Hello " + escape(investorName) + ", your investment payment was received successfully.</p>"
                    + "<p><b>Invoice:</b> " + escape(invoiceNumber) + "<br/><b>Amount:</b> ₹" + escape(amount)
                    + "<br/><b>Razorpay payment ID:</b> " + escape(paymentId) + "</p>"
                    + "<p><a href='" + escape(invoiceUrl) + "' style='background:#1261e8;color:white;padding:12px 18px;border-radius:8px;text-decoration:none'>Download invoice</a></p>"
                    + "<p style='color:#64748b;font-size:12px'>This is a system-generated payment invoice from Anusha Trade.</p></div></div>", true);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            System.err.println("[EmailService] Payment invoice delivery failed: " + ex.getMessage());
            return false;
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private boolean send(String to, String subject, String body) {
        if (!enabled || mailSender == null) {
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
