package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.CouponRedemption;
import com.anushabazaar.backend.domain.DomainEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, String> {
    long countByCouponIdAndStatusIn(String couponId, List<DomainEnums.CouponRedemptionStatus> statuses);
    long countByCouponIdAndUserIdAndStatusIn(String couponId, String userId, List<DomainEnums.CouponRedemptionStatus> statuses);
    Optional<CouponRedemption> findByInvestmentId(String investmentId);
}
