package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, String> {
    List<WithdrawalRequest> findByInvestorIdOrderByRequestedAtDesc(String investorId);
    List<WithdrawalRequest> findByUserIdOrderByRequestedAtDesc(String userId);
    List<WithdrawalRequest> findByUserId(String userId);
    List<WithdrawalRequest> findByStatus(DomainEnums.WithdrawalStatus status);
}
