package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.ReferralRelationship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRelationshipRepository extends JpaRepository<ReferralRelationship, String> {
    List<ReferralRelationship> findByReferrerUserIdOrderByReferralLevelAscLinkedAtDesc(String referrerUserId);
    List<ReferralRelationship> findByReferredUserIdOrderByReferralLevelAsc(String referredUserId);
}
