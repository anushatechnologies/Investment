package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.Investment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentRepository extends JpaRepository<Investment, String> {
    List<Investment> findByInvestorUserId(String investorUserId);
    List<Investment> findByStatus(DomainEnums.InvestmentStatus status);
    List<Investment> findByStatusAndReceiptApproved(DomainEnums.InvestmentStatus status, boolean receiptApproved);
}
