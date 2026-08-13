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
                InvestmentPlan plan1 = new InvestmentPlan();
                plan1.setId(UUID.randomUUID().toString());
                plan1.setPlanName("Anusha Standard Growth Plan");
                plan1.setDescription("Standard 6-Month Lock-in Investment Plan with 10% Monthly Payout credited to Wallet.");
                plan1.setMinimumAmount(new BigDecimal("10000"));
                plan1.setMaximumAmount(new BigDecimal("1000000"));
                plan1.setLockInMonths(6);
                plan1.setMonthlyInterestRate(new BigDecimal("10.0"));
                plan1.setActive(true);
                plan1.setCreatedByAdminId("SYSTEM");
                plan1.setCreatedAt(LocalDateTime.now());
                plan1.setLastModifiedAt(LocalDateTime.now());
                plan1.setLastModifiedBy("SYSTEM");
                planRepository.save(plan1);

                InvestmentPlan plan2 = new InvestmentPlan();
                plan2.setId(UUID.randomUUID().toString());
                plan2.setPlanName("Anusha Prime Investor Plan");
                plan2.setDescription("High-Yield 12-Month Lock-in Investment Plan with 12% Monthly Payout for high net-worth investors.");
                plan2.setMinimumAmount(new BigDecimal("100000"));
                plan2.setMaximumAmount(new BigDecimal("5000000"));
                plan2.setLockInMonths(12);
                plan2.setMonthlyInterestRate(new BigDecimal("12.0"));
                plan2.setActive(true);
                plan2.setCreatedByAdminId("SYSTEM");
                plan2.setCreatedAt(LocalDateTime.now());
                plan2.setLastModifiedAt(LocalDateTime.now());
                plan2.setLastModifiedBy("SYSTEM");
                planRepository.save(plan2);
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
        user.setRole(role);
        user.setRiskDisclosureAccepted(true);
        user.setRiskDisclosureDate(LocalDateTime.now());
        user.setInvestorAgreementAccepted(true);
        user.setInvestorAgreementDate(LocalDateTime.now());
        user.setEmailVerified(true);
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
