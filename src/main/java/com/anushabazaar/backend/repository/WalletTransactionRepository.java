package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<WalletTransaction> findFirstByReferenceId(String referenceId);
}
