package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, String> {
    Optional<PaymentReceipt> findTopByInvestmentIdOrderByUploadedAtDesc(String investmentId);
    List<PaymentReceipt> findByVerificationStatus(DomainEnums.ReceiptStatus verificationStatus);
}
