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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ReceiptService {

    private final PaymentReceiptRepository receiptRepository;
    private final InvestmentRepository investmentRepository;

    public ReceiptService(PaymentReceiptRepository receiptRepository, InvestmentRepository investmentRepository) {
        this.receiptRepository = receiptRepository;
        this.investmentRepository = investmentRepository;
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
        if (receipt.getEmailStatus() == null || receipt.getEmailStatus() == DomainEnums.EmailStatus.NOT_SENT) {
            receipt.setEmailStatus(DomainEnums.EmailStatus.SENT);
        }
        receiptRepository.save(receipt);
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
            receipt.setReceiptUrl("https://storage.anusha.trade/receipts/" + receipt.getReceiptNumber() + ".pdf");
            updated = true;
        }
        if (updated) receiptRepository.save(receipt);
    }
}
