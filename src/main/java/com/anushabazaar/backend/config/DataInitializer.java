package com.anushabazaar.backend.config;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.InvestmentPlan;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.domain.Wallet;
import com.anushabazaar.backend.repository.InvestmentPlanRepository;
import com.anushabazaar.backend.repository.UserRepository;
import com.anushabazaar.backend.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               WalletRepository walletRepository,
                               InvestmentPlanRepository planRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("superadmin@anushabazaar.com").isEmpty()) {
                User superAdmin = baseAdmin("SUPER_ADMIN", "superadmin@anushabazaar.com", DomainEnums.Role.SUPER_ADMIN, passwordEncoder);
                userRepository.save(superAdmin);
                walletRepository.save(newWallet(superAdmin.getId()));
            }
            if (userRepository.findByEmail("admin@anushabazaar.com").isEmpty()) {
                User admin = baseAdmin("ADMIN", "admin@anushabazaar.com", DomainEnums.Role.ADMIN, passwordEncoder);
                userRepository.save(admin);
                walletRepository.save(newWallet(admin.getId()));
            }
            if (planRepository.count() == 0) {
                InvestmentPlan plan = new InvestmentPlan();
                plan.setId(UUID.randomUUID().toString());
                plan.setPlanName("Gold Plan");
                plan.setDescription("Default seeded investment plan");
                plan.setMinimumAmount(new BigDecimal("5000"));
                plan.setMaximumAmount(new BigDecimal("1000000"));
                plan.setLockInMonths(6);
                plan.setMonthlyInterestRate(new BigDecimal("1.5"));
                plan.setActive(true);
                plan.setCreatedByAdminId("SYSTEM");
                plan.setCreatedAt(LocalDateTime.now());
                plan.setLastModifiedAt(LocalDateTime.now());
                plan.setLastModifiedBy("SYSTEM");
                planRepository.save(plan);
            }
        };
    }

    private User baseAdmin(String name, String email, DomainEnums.Role role, PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFullName(name);
        user.setEmail(email);
        user.setMobileNumber(role == DomainEnums.Role.ADMIN ? "9000000001" : "9000000000");
        user.setPasswordHash(passwordEncoder.encode("Admin@123"));
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));
        user.setPanNumber("ABCDE1234F");
        user.setAadhaarLast4("1234");
        user.setAddress("Mumbai");
        user.setBankAccountNumber("1234567890");
        user.setBankIfscCode("SBIN0001234");
        user.setBankName("State Bank of India");
        user.setReferralCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        user.setKycStatus(DomainEnums.KycStatus.APPROVED);
        user.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.ACTIVE);
        user.setRole(role);
        user.setRiskDisclosureAccepted(true);
        user.setRiskDisclosureDate(LocalDateTime.now());
        user.setInvestorAgreementAccepted(true);
        user.setInvestorAgreementDate(LocalDateTime.now());
        user.setEmailVerified(true);
        user.setTermsAccepted(true);
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setPrivacyPolicyAccepted(true);
        user.setPrivacyPolicyAcceptedAt(LocalDateTime.now());
        user.setKycConsentAccepted(true);
        user.setKycConsentAcceptedAt(LocalDateTime.now());
        user.setBankVerified(true);
        user.setBankVerifiedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy("SYSTEM");
        return user;
    }

    private Wallet newWallet(String userId) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID().toString());
        wallet.setUserId(userId);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setLockedBalance(BigDecimal.ZERO);
        wallet.setTotalCredited(BigDecimal.ZERO);
        wallet.setTotalDebited(BigDecimal.ZERO);
        wallet.setVersionValue(0L);
        wallet.setLastUpdatedAt(LocalDateTime.now());
        return wallet;
    }
}
