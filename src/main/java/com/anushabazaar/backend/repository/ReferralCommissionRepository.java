package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.ReferralCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralCommissionRepository extends JpaRepository<ReferralCommission, String> {
    List<ReferralCommission> findByBeneficiaryUserIdOrderByCreditedAtDesc(String beneficiaryUserId);
}
