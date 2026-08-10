package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.InvestmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentPlanRepository extends JpaRepository<InvestmentPlan, String> {
    List<InvestmentPlan> findByActiveTrue();
}
