package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.TokenRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRecordRepository extends JpaRepository<TokenRecord, String> {
    Optional<TokenRecord> findByTokenValueAndTokenTypeAndUsedFalse(String tokenValue, DomainEnums.TokenType tokenType);
    List<TokenRecord> findByUserIdAndTokenTypeAndUsedFalse(String userId, DomainEnums.TokenType tokenType);
}
