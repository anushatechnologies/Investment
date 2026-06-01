package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.Coupon;
import com.anushabazaar.backend.domain.DomainEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    List<Coupon> findByStatusOrderByCreatedAtDesc(DomainEnums.CouponStatus status);
    List<Coupon> findByStatusAndValidUntilAfterOrderByCreatedAtDesc(DomainEnums.CouponStatus status, LocalDateTime validUntil);
}
