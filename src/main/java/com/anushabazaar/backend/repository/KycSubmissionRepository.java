package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.KycSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycSubmissionRepository extends JpaRepository<KycSubmission, String> {
    Optional<KycSubmission> findTopByUserIdOrderBySubmittedAtDesc(String userId);
    List<KycSubmission> findByStatus(DomainEnums.KycStatus status);
}
