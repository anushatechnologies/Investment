package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.InterestRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestRecordRepository extends JpaRepository<InterestRecord, String> {
    List<InterestRecord> findByInvestorIdOrderByCalculatedAtDesc(String investorId);
    boolean existsByInvestmentIdAndCalculationMonth(String investmentId, String calculationMonth);
}
