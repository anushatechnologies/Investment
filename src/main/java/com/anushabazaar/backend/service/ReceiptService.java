package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.Investment;
import com.anushabazaar.backend.domain.PaymentReceipt;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.repository.InvestmentRepository;
import com.anushabazaar.backend.repository.PaymentReceiptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ReceiptService {

    private final PaymentReceiptRepository receiptRepository;
    private final InvestmentRepository investmentRepository;
    private final EmailService emailService;
    private final String invoiceBaseUrl;
    private final String logoUrl;

    public ReceiptService(PaymentReceiptRepository receiptRepository, InvestmentRepository investmentRepository,
                          EmailService emailService,
                          @Value("${app.email.invoice-base-url:http://localhost:8080}") String invoiceBaseUrl,
                          @Value("${app.email.logo-url:https://anusha.trade/assets/brand-logo.png}") String logoUrl) {
        this.receiptRepository = receiptRepository;
        this.investmentRepository = investmentRepository;
        this.emailService = emailService;
        this.invoiceBaseUrl = invoiceBaseUrl;
        this.logoUrl = logoUrl;
    }

    public ApiDtos.ReceiptStatusResponse getReceiptStatus(User user, String investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investment not found"));

        if (!user.getRole().name().contains("ADMIN") && !investment.getInvestorUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to investment receipt");
        }

        PaymentReceipt receipt = receiptRepository.findTopByInvestmentIdOrderByUploadedAtDesc(investmentId).orElse(null);
        String paymentStatus = mapInvestmentToPaymentStatus(investment.getStatus());
        if (receipt == null) {
            return new ApiDtos.ReceiptStatusResponse(paymentStatus, investment.getId(), investment.getInvestmentAmount(),
                    new ApiDtos.ReceiptStatusDetails(null, null, DomainEnums.EmailStatus.NOT_SENT.name(), false));
        }

        ensureReceiptNumberAndUrl(receipt);
        String emailStatus = receipt.getEmailStatus() != null ? receipt.getEmailStatus().name() : DomainEnums.EmailStatus.NOT_SENT.name();
        return new ApiDtos.ReceiptStatusResponse(paymentStatus, investment.getId(), investment.getInvestmentAmount(),
                new ApiDtos.ReceiptStatusDetails(receipt.getReceiptNumber(), receipt.getReceiptUrl(), emailStatus, true));
    }

    public void triggerReceiptDelivery(PaymentReceipt receipt, User user) {
        ensureReceiptNumberAndUrl(receipt);
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()
                && (receipt.getEmailStatus() == null || receipt.getEmailStatus() == DomainEnums.EmailStatus.NOT_SENT
                || receipt.getEmailStatus() == DomainEnums.EmailStatus.FAILED)) {
            receipt.setEmailStatus(DomainEnums.EmailStatus.SENDING);
            receiptRepository.save(receipt);
            boolean sent = emailService.sendPaymentInvoice(user.getEmail(), user.getFullName(), receipt.getReceiptNumber(),
                    receipt.getPaymentAmount() == null ? "0.00" : receipt.getPaymentAmount().toPlainString(),
                    receipt.getBankReference(), invoiceBaseUrl + "/api/receipts/" + receipt.getInvestmentId() + "/invoice");
            receipt.setEmailStatus(sent ? DomainEnums.EmailStatus.SENT : DomainEnums.EmailStatus.FAILED);
        } else if (receipt.getEmailStatus() == null) {
            receipt.setEmailStatus(DomainEnums.EmailStatus.NOT_SENT);
        }
        receiptRepository.save(receipt);
    }

    public String renderInvoice(User user, String investmentId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investment not found"));
        if (!user.getRole().name().contains("ADMIN") && !investment.getInvestorUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to invoice");
        }
        PaymentReceipt receipt = receiptRepository.findTopByInvestmentIdOrderByUploadedAtDesc(investmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not available"));
        ensureReceiptNumberAndUrl(receipt);
        String name = user.getFullName() == null ? "Investor" : user.getFullName();
        String email = user.getEmail() == null ? "" : user.getEmail();
        return "<!doctype html><html><head><meta charset='utf-8'><title>" + receipt.getReceiptNumber() + "</title>"
                + "<style>body{font-family:Arial;color:#10213a;margin:0;padding:32px;background:#f5f7fb}.invoice{max-width:760px;margin:auto;background:#fff;padding:36px;border-radius:16px;box-shadow:0 8px 30px #dbe3f0}.brand{background:#1261e8;color:#fff;padding:22px;border-radius:12px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-top:24px}.label{color:#64748b;font-size:12px;text-transform:uppercase}.value{font-weight:bold;margin-top:6px}hr{border:0;border-top:1px solid #e5e7eb;margin:24px 0}</style></head>"
                + "<body><div class='invoice'><div class='brand'><img src='" + escape(logoUrl) + "' style='height:42px;vertical-align:middle;margin-right:12px' alt='Anusha Trade logo'><b style='font-size:28px;vertical-align:middle'>ANUSHA TRADE</b><div>INVESTMENT PAYMENT INVOICE</div></div>"
                + "<div class='grid'><div><div class='label'>Invoice number</div><div class='value'>" + receipt.getReceiptNumber() + "</div></div>"
                + "<div><div class='label'>Payment date</div><div class='value'>" + receipt.getPaymentDate() + "</div></div>"
                + "<div><div class='label'>Investor</div><div class='value'>" + escape(name) + "</div></div>"
                + "<div><div class='label'>Email</div><div class='value'>" + escape(email) + "</div></div></div><hr>"
                + "<div class='grid'><div><div class='label'>Amount received</div><div class='value'>INR " + receipt.getPaymentAmount() + "</div></div>"
                + "<div><div class='label'>Payment status</div><div class='value'>SUCCESS</div></div></div>"
                + "<hr><p>System-generated invoice. This document confirms receipt of the investment payment.</p></div></body></html>";
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String mapInvestmentToPaymentStatus(DomainEnums.InvestmentStatus status) {
        if (status == DomainEnums.InvestmentStatus.ACTIVE || status == DomainEnums.InvestmentStatus.MATURED) return "SUCCESS";
        if (status == DomainEnums.InvestmentStatus.REJECTED || status == DomainEnums.InvestmentStatus.CANCELLED) return "FAILED";
        return "PENDING";
    }

    private void ensureReceiptNumberAndUrl(PaymentReceipt receipt) {
        boolean updated = false;
        if (receipt.getReceiptNumber() == null || receipt.getReceiptNumber().isBlank()) {
            receipt.setReceiptNumber("ATR-2026-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
            updated = true;
        }
        if (receipt.getReceiptUrl() == null || receipt.getReceiptUrl().isBlank()) {
            receipt.setReceiptUrl("/api/receipts/" + receipt.getInvestmentId() + "/invoice");
            updated = true;
        }
        if (updated) receiptRepository.save(receipt);
    }
}
