package com.anushabazaar.backend.config;

import com.anushabazaar.backend.domain.DomainEnums;
import com.anushabazaar.backend.domain.InvestmentPlan;
import com.anushabazaar.backend.domain.User;
import com.anushabazaar.backend.domain.Wallet;
import com.anushabazaar.backend.repository.InvestmentPlanRepository;
import com.anushabazaar.backend.repository.UserRepository;
import com.anushabazaar.backend.repository.WalletRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
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
                               @Value("${app.seed.default-admins:true}") boolean seedDefaultAdmins,
                               @Value("${app.seed.admin.email:}") String adminEmail,
                               @Value("${app.seed.admin.password:}") String adminPassword,
                               @Value("${app.seed.admin.full-name:ADMIN}") String adminFullName,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (seedDefaultAdmins) {
                seedAdmin(userRepository, walletRepository, passwordEncoder,
                        "SUPER_ADMIN", "superadmin@anushabazaar.com", "Admin@123", "9000000000",
                        DomainEnums.Role.SUPER_ADMIN, false);
                seedAdmin(userRepository, walletRepository, passwordEncoder,
                        "ADMIN", "admin@anushabazaar.com", "Admin@123", "9000000001",
                        DomainEnums.Role.ADMIN, false);
            }

            if (!isBlank(adminEmail) && !isBlank(adminPassword)) {
                seedAdmin(userRepository, walletRepository, passwordEncoder,
                        adminFullName, adminEmail, adminPassword, "9000000001",
                        DomainEnums.Role.ADMIN, true);
            }

            if (planRepository.count() == 0) {
                InvestmentPlan plan = new InvestmentPlan();
                plan.setId(UUID.randomUUID().toString());
                plan.setPlanName("Anusha Milk Trade");
                plan.setDescription("Monthly income plan with admin-managed returns.");
                plan.setMinimumAmount(new BigDecimal("5000"));
                plan.setMaximumAmount(new BigDecimal("1000000"));
                plan.setLockInMonths(6);
                plan.setMonthlyInterestRate(new BigDecimal("10"));
                plan.setActive(true);
                plan.setCreatedByAdminId("SYSTEM");
                plan.setCreatedAt(LocalDateTime.now());
                plan.setLastModifiedAt(LocalDateTime.now());
                plan.setLastModifiedBy("SYSTEM");
                planRepository.save(plan);
            } else {
                planRepository.findAll().stream()
                        .filter(plan -> "Gold Plan".equalsIgnoreCase(plan.getPlanName()))
                        .forEach(plan -> {
                            plan.setPlanName("Anusha Milk Trade");
                            plan.setDescription("Monthly income plan with admin-managed returns.");
                            plan.setMonthlyInterestRate(new BigDecimal("10"));
                            plan.setLastModifiedAt(LocalDateTime.now());
                            plan.setLastModifiedBy("SYSTEM_MIGRATION");
                            planRepository.save(plan);
                        });
            }
        };
    }

    private void seedAdmin(UserRepository userRepository,
                           WalletRepository walletRepository,
                           PasswordEncoder passwordEncoder,
                           String name,
                           String email,
                           String password,
                           String mobileNumber,
                           DomainEnums.Role role,
                           boolean updatePassword) {
        userRepository.findByEmail(email).ifPresentOrElse(existing -> {
            existing.setFullName(name);
            existing.setRole(role);
            existing.setAccountStatus(DomainEnums.AccountStatus.ACTIVE);
            existing.setOnboardingStatus(DomainEnums.OnboardingStatus.ACTIVE);
            existing.setEmailVerified(true);
            existing.setUpdatedAt(LocalDateTime.now());
            if (updatePassword) {
                existing.setPasswordHash(passwordEncoder.encode(password));
            }
            userRepository.save(existing);
            ensureWallet(walletRepository, existing.getId());
        }, () -> {
            User admin = baseAdmin(name, email, password, mobileNumber, role, passwordEncoder);
            userRepository.save(admin);
            walletRepository.save(newWallet(admin.getId()));
        });
    }

    private User baseAdmin(String name,
                           String email,
                           String password,
                           String mobileNumber,
                           DomainEnums.Role role,
                           PasswordEncoder passwordEncoder) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFullName(name);
        user.setEmail(email);
        user.setMobileNumber(mobileNumber);
        user.setPasswordHash(passwordEncoder.encode(password));
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

    private void ensureWallet(WalletRepository walletRepository, String userId) {
        if (walletRepository.findByUserId(userId).isEmpty()) {
            walletRepository.save(newWallet(userId));
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
