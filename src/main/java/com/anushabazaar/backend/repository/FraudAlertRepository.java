package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, String> {
    List<FraudAlert> findByStatusOrderByCreatedAtDesc(DomainEnums.AlertStatus status);
    List<FraudAlert> findAllByOrderByCreatedAtDesc();
}
