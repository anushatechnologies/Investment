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

            // 1. Anusha Milk Trade Investment Plan (Min ₹5,000 to Max ₹10,00,000, 10% monthly interest)
            ensurePlan(planRepository,
                    "PLAN-MILK-TRADE-5K-10L",
                    "Anusha Milk Trade Investment Plan",
                    "Official Anusha Milk Trade high-yield investment plan with 10% monthly payout credited directly to your wallet.",
                    new BigDecimal("5000"),
                    new BigDecimal("1000000"),
                    6,
                    new BigDecimal("10.0")
            );

            // 2. ₹1 Razorpay Test Payment Plan (Min ₹1 to Max ₹100, 10% monthly interest)
            ensurePlan(planRepository,
                    "PLAN-RAZORPAY-TEST-1INR",
                    "₹1 Razorpay Test Payment Plan",
                    "Instant ₹1 test investment plan to verify real-time Razorpay payments, UPI, and instant digital receipts.",
                    new BigDecimal("1"),
                    new BigDecimal("100"),
                    1,
                    new BigDecimal("10.0")
            );

            // 3. Anusha Prime Investor Plan (Min ₹1,00,000 to Max ₹50,00,000, 12% monthly interest)
            ensurePlan(planRepository,
                    "PLAN-PRIME-INVESTOR-1L-50L",
                    "Anusha Prime Investor Plan",
                    "High-Yield 12-Month Lock-in Investment Plan with 12% Monthly Payout for high net-worth investors.",
                    new BigDecimal("100000"),
                    new BigDecimal("5000000"),
                    12,
                    new BigDecimal("12.0")
            );
        };
    }

    private void ensurePlan(InvestmentPlanRepository planRepository,
                            String id,
                            String name,
                            String description,
                            BigDecimal minAmount,
                            BigDecimal maxAmount,
                            int lockInMonths,
                            BigDecimal monthlyRate) {
        InvestmentPlan plan = planRepository.findById(id).orElseGet(() -> {
            InvestmentPlan p = new InvestmentPlan();
            p.setId(id);
            p.setCreatedAt(LocalDateTime.now());
            p.setCreatedByAdminId("SYSTEM");
            return p;
        });
        plan.setPlanName(name);
        plan.setDescription(description);
        plan.setMinimumAmount(minAmount);
        plan.setMaximumAmount(maxAmount);
        plan.setLockInMonths(lockInMonths);
        plan.setMonthlyInterestRate(monthlyRate);
        plan.setActive(true);
        plan.setLastModifiedAt(LocalDateTime.now());
        plan.setLastModifiedBy("SYSTEM");
        planRepository.save(plan);
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
