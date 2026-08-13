package com.anushabazaar.backend.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    Optional<BankAccount> findByUserId(String userId);
    Optional<BankAccount> findTopByUserIdOrderByCreatedAtDesc(String userId);
    List<BankAccount> findByVerifiedTrue();
}
