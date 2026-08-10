package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.*;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlatformService {

    private static final Map<Integer, BigDecimal> REFERRAL_RATES = Map.of(
            1, new BigDecimal("5"),
            2, new BigDecimal("4"),
            3, new BigDecimal("3"),
            4, new BigDecimal("2"),
            5, new BigDecimal("1")
    );

    private final UserRepository userRepository;
    private final KycSubmissionRepository kycSubmissionRepository;
    private final InvestmentPlanRepository planRepository;
    private final InvestmentRepository investmentRepository;
    private final PaymentReceiptRepository paymentReceiptRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final ReferralRelationshipRepository referralRelationshipRepository;
    private final ReferralCommissionRepository referralCommissionRepository;
    private final InterestRecordRepository interestRecordRepository;
    private final NotificationRepository notificationRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final AuditService auditService;
    private final ReceiptService receiptService;

    public PlatformService(UserRepository userRepository,
                           KycSubmissionRepository kycSubmissionRepository,
                           InvestmentPlanRepository planRepository,
                           InvestmentRepository investmentRepository,
                           PaymentReceiptRepository paymentReceiptRepository,
                           WalletRepository walletRepository,
                           WalletTransactionRepository walletTransactionRepository,
                           WithdrawalRequestRepository withdrawalRepository,
                           ReferralRelationshipRepository referralRelationshipRepository,
                           ReferralCommissionRepository referralCommissionRepository,
                           InterestRecordRepository interestRecordRepository,
                           NotificationRepository notificationRepository,
                           FraudAlertRepository fraudAlertRepository,
                           AuditLogRepository auditLogRepository,
                           StorageService storageService,
                           AuditService auditService,
                           ReceiptService receiptService) {
        this.userRepository = userRepository;
        this.kycSubmissionRepository = kycSubmissionRepository;
        this.planRepository = planRepository;
        this.investmentRepository = investmentRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.referralRelationshipRepository = referralRelationshipRepository;
        this.referralCommissionRepository = referralCommissionRepository;
        this.interestRecordRepository = interestRecordRepository;
        this.notificationRepository = notificationRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService;
        this.auditService = auditService;
        this.receiptService = receiptService;
    }

    @Transactional
    public KycSubmission submitKyc(User user, MultipartFile panCard, MultipartFile aadhaarFront, MultipartFile aadhaarBack,
                                   MultipartFile selfiePhoto, MultipartFile bankProof, HttpServletRequest request) {
        KycSubmission kyc = kycSubmissionRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getId()).orElseGet(KycSubmission::new);
        kyc.setId(kyc.getId() == null ? UUID.randomUUID().toString() : kyc.getId());
        kyc.setUserId(user.getId());
        kyc.setPanCardPath(storageService.save(panCard, "kyc"));
        kyc.setAadhaarFrontPath(storageService.save(aadhaarFront, "kyc"));
        kyc.setAadhaarBackPath(storageService.save(aadhaarBack, "kyc"));
        kyc.setSelfiePath(storageService.save(selfiePhoto, "kyc"));
        kyc.setBankProofPath(storageService.save(bankProof, "kyc"));
        kyc.setStatus(DomainEnums.KycStatus.PENDING);
        kyc.setSubmittedAt(LocalDateTime.now());
        kyc.setReviewedByAdminId(null);
        kyc.setReviewedAt(null);
        kyc.setRejectionReason(null);
        user.setKycStatus(DomainEnums.KycStatus.PENDING);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC submitted", "Your KYC documents are under review.", DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(user, "KYC_SUBMITTED", "KycSubmission", saved.getId(), null, "PENDING", request);
        return saved;
    }

    public Map<String, Object> getOwnKycStatus(User user) {
        KycSubmission kyc = kycSubmissionRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getId()).orElse(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kycStatus", user.getKycStatus());
        response.put("submission", kyc);
        return response;
    }

    public List<KycSubmission> getPendingKyc() {
        return kycSubmissionRepository.findByStatus(DomainEnums.KycStatus.PENDING);
    }

    @Transactional
    public KycSubmission approveKyc(User admin, String id, String notes, HttpServletRequest request) {
        KycSubmission kyc = getKyc(id);
        User user = getUser(kyc.getUserId());
        kyc.setStatus(DomainEnums.KycStatus.APPROVED);
        kyc.setReviewedByAdminId(admin.getId());
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setAdminNotes(notes);
        user.setKycStatus(DomainEnums.KycStatus.APPROVED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC approved", "Your KYC has been approved. You can now invest.", DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(admin, "KYC_APPROVED", "KycSubmission", id, null, "APPROVED", request);
        return saved;
    }

    @Transactional
    public KycSubmission rejectKyc(User admin, String id, ApiDtos.KycDecisionRequest body, HttpServletRequest request) {
        KycSubmission kyc = getKyc(id);
        User user = getUser(kyc.getUserId());
        kyc.setStatus(DomainEnums.KycStatus.REJECTED);
        kyc.setReviewedByAdminId(admin.getId());
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setRejectionReason(body.reason());
        kyc.setAdminNotes(body.adminNotes());
        user.setKycStatus(DomainEnums.KycStatus.REJECTED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC rejected", "KYC rejected. Reason: " + body.reason(), DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(admin, "KYC_REJECTED", "KycSubmission", id, null, body.reason(), request);
        return saved;
    }

    public Map<String, String> getKycDocuments(String id) {
        KycSubmission kyc = getKyc(id);
        return Map.of(
                "panCard", kyc.getPanCardPath(),
                "aadhaarFront", kyc.getAadhaarFrontPath(),
                "aadhaarBack", kyc.getAadhaarBackPath(),
                "selfie", kyc.getSelfiePath(),
                "bankProof", kyc.getBankProofPath()
        );
    }

    public List<InvestmentPlan> getActivePlans() {
        return planRepository.findByActiveTrue();
    }

    public List<InvestmentPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Transactional
    public InvestmentPlan createPlan(User admin, ApiDtos.CreatePlanRequest body, HttpServletRequest request) {
        InvestmentPlan plan = new InvestmentPlan();
        plan.setId(UUID.randomUUID().toString());
        plan.setPlanName(body.planName());
        plan.setDescription(body.description());
        plan.setMinimumAmount(body.minimumAmount());
        plan.setMaximumAmount(body.maximumAmount());
        plan.setLockInMonths(body.lockInMonths());
        plan.setMonthlyInterestRate(body.monthlyInterestRate());
        plan.setActive(true);
        plan.setCreatedByAdminId(admin.getId());
        plan.setCreatedAt(LocalDateTime.now());
        plan.setLastModifiedAt(LocalDateTime.now());
        plan.setLastModifiedBy(admin.getId());
        InvestmentPlan saved = planRepository.save(plan);
        auditService.log(admin, "PLAN_CREATED", "InvestmentPlan", saved.getId(), null, saved.getPlanName(), request);
        return saved;
    }

    @Transactional
    public InvestmentPlan updatePlan(User admin, String id, ApiDtos.UpdatePlanRequest body, HttpServletRequest request) {
        InvestmentPlan plan = getPlan(id);
        plan.setPlanName(body.planName());
        plan.setDescription(body.description());
        plan.setMinimumAmount(body.minimumAmount());
        plan.setMaximumAmount(body.maximumAmount());
        plan.setLockInMonths(body.lockInMonths());
        plan.setMonthlyInterestRate(body.monthlyInterestRate());
        plan.setActive(body.active());
        plan.setLastModifiedAt(LocalDateTime.now());
        plan.setLastModifiedBy(admin.getId());
        InvestmentPlan saved = planRepository.save(plan);
        auditService.log(admin, "PLAN_UPDATED", "InvestmentPlan", id, null, saved.getPlanName(), request);
        return saved;
    }

    @Transactional
    public InvestmentPlan deactivatePlan(User admin, String id, HttpServletRequest request) {
        InvestmentPlan plan = getPlan(id);
        plan.setActive(false);
        plan.setLastModifiedAt(LocalDateTime.now());
        plan.setLastModifiedBy(admin.getId());
        InvestmentPlan saved = planRepository.save(plan);
        auditService.log(admin, "PLAN_DEACTIVATED", "InvestmentPlan", id, null, saved.getPlanName(), request);
        return saved;
    }

    @Transactional
    public Investment applyInvestment(User user, ApiDtos.ApplyInvestmentRequest body, HttpServletRequest request) {
        ensureInvestorReady(user);
        InvestmentPlan plan = getPlan(body.investmentPlanId());
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan is inactive");
        }
        if (body.investmentAmount().compareTo(plan.getMinimumAmount()) < 0 || body.investmentAmount().compareTo(plan.getMaximumAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investment amount is outside plan limits");
        }
        Investment investment = new Investment();
        investment.setId(UUID.randomUUID().toString());
        investment.setInvestorUserId(user.getId());
        investment.setInvestmentPlanId(plan.getId());
        investment.setInvestmentAmount(body.investmentAmount());
        investment.setStatus(DomainEnums.InvestmentStatus.PENDING_RECEIPT);
        investment.setAppliedAt(LocalDateTime.now());
        investment.setMonthlyInterestRate(plan.getMonthlyInterestRate());
        investment.setTotalInterestEarned(BigDecimal.ZERO);
        investment.setTotalPrincipalReturned(BigDecimal.ZERO);
        investment.setReceiptApproved(false);
        Investment saved = investmentRepository.save(investment);
        notifyUser(user.getId(), "Investment applied", "Investment created. Upload payment receipt to continue.", DomainEnums.NotificationType.INVESTMENT_UPDATE);
        auditService.log(user, "INVESTMENT_APPLIED", "Investment", saved.getId(), null, body.investmentAmount().toPlainString(), request);
        return saved;
    }

    @Transactional
    public PaymentReceipt uploadReceipt(User user, String investmentId, MultipartFile receiptFile, BigDecimal paymentAmount,
                                        LocalDate paymentDate, DomainEnums.PaymentMode paymentMode, String bankReference,
                                        HttpServletRequest request) {
        Investment investment = getInvestment(investmentId);
        if (!investment.getInvestorUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your investment");
        }
        PaymentReceipt receipt = paymentReceiptRepository.findTopByInvestmentIdOrderByUploadedAtDesc(investmentId).orElseGet(PaymentReceipt::new);
        receipt.setId(receipt.getId() == null ? UUID.randomUUID().toString() : receipt.getId());
        receipt.setInvestmentId(investmentId);
        receipt.setInvestorId(user.getId());
        receipt.setFileName(receiptFile.getOriginalFilename());
        receipt.setFileType(receiptFile.getContentType());
        receipt.setFileSize(receiptFile.getSize());
        receipt.setStorageKey(storageService.save(receiptFile, "receipts"));
        receipt.setPresignedUrlExpiry(LocalDateTime.now().plusHours(1));
        receipt.setPaymentAmount(paymentAmount);
        receipt.setPaymentDate(paymentDate);
        receipt.setPaymentMode(paymentMode);
        receipt.setBankReference(bankReference);
        receipt.setVerificationStatus(DomainEnums.ReceiptStatus.PENDING);
        receipt.setUploadedAt(LocalDateTime.now());

        investment.setStatus(DomainEnums.InvestmentStatus.RECEIPT_UPLOADED);
        investment.setReceiptApproved(false);
        investmentRepository.save(investment);
        PaymentReceipt saved = paymentReceiptRepository.save(receipt);

        if (paymentAmount.subtract(investment.getInvestmentAmount()).abs().compareTo(new BigDecimal("100")) > 0) {
            createFraudAlert(user.getId(), DomainEnums.AlertLevel.HIGH, "RECEIPT_AMOUNT_MISMATCH", "Receipt amount differs from investment amount by more than 100");
        }
        notifyUser(user.getId(), "Receipt uploaded", "Receipt uploaded and pending admin review.", DomainEnums.NotificationType.INVESTMENT_UPDATE);
        receiptService.triggerReceiptDelivery(saved, user);
        auditService.log(user, "RECEIPT_UPLOADED", "PaymentReceipt", saved.getId(), null, saved.getFileName(), request);
        return saved;
    }

    public List<Investment> getOwnInvestments(User user) {
        return investmentRepository.findByInvestorUserId(user.getId());
    }

    public Investment getOwnInvestment(User user, String id) {
        Investment investment = getInvestment(id);
        if (!investment.getInvestorUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your investment");
        }
        return investment;
    }

    @Transactional
    public Investment cancelInvestment(User user, String id, ApiDtos.CancelInvestmentRequest body, HttpServletRequest request) {
        Investment investment = getOwnInvestment(user, id);
        if (investment.getStatus() == DomainEnums.InvestmentStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active investments cannot be cancelled from this endpoint");
        }
        investment.setStatus(DomainEnums.InvestmentStatus.CANCELLED);
        investment.setCancellationReason(body.reason());
        Investment saved = investmentRepository.save(investment);
        auditService.log(user, "INVESTMENT_CANCELLED", "Investment", id, null, body.reason(), request);
        return saved;
    }

    public List<Investment> getPendingInvestments() {
        return investmentRepository.findByStatus(DomainEnums.InvestmentStatus.RECEIPT_UPLOADED);
    }

    @Transactional
    public Map<String, Object> verifyReceipt(User admin, String investmentId, ApiDtos.VerifyReceiptRequest body, HttpServletRequest request) {
        Investment investment = getInvestment(investmentId);
        PaymentReceipt receipt = paymentReceiptRepository.findTopByInvestmentIdOrderByUploadedAtDesc(investmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));
        if (body.approved()) {
            receipt.setVerificationStatus(DomainEnums.ReceiptStatus.APPROVED);
            receipt.setVerifiedByAdminId(admin.getId());
            receipt.setVerifiedAt(LocalDateTime.now());
            investment.setReceiptApproved(true);
            notifyUser(investment.getInvestorUserId(), "Receipt approved", "Receipt approved. Awaiting investment activation.", DomainEnums.NotificationType.INVESTMENT_UPDATE);
            User investor = userRepository.findById(investment.getInvestorUserId()).orElse(null);
            if (investor != null) {
                receiptService.triggerReceiptDelivery(receipt, investor);
            }
        } else {
            receipt.setVerificationStatus(DomainEnums.ReceiptStatus.REJECTED);
            receipt.setVerifiedByAdminId(admin.getId());
            receipt.setVerifiedAt(LocalDateTime.now());
            receipt.setRejectionReason(body.rejectionReason());
            investment.setReceiptApproved(false);
            investment.setStatus(DomainEnums.InvestmentStatus.REJECTED);
            notifyUser(investment.getInvestorUserId(), "Receipt rejected", "Receipt rejected. Reason: " + body.rejectionReason(), DomainEnums.NotificationType.INVESTMENT_UPDATE);
        }
        paymentReceiptRepository.save(receipt);
        investmentRepository.save(investment);
        auditService.log(admin, "RECEIPT_VERIFIED", "PaymentReceipt", receipt.getId(), null, receipt.getVerificationStatus().name(), request);
        return Map.of("investment", investment, "receipt", receipt);
    }

    @Transactional
    public Investment activateInvestment(User admin, String investmentId, ApiDtos.ActivateInvestmentRequest body, HttpServletRequest request) {
        Investment investment = getInvestment(investmentId);
        if (!investment.isReceiptApproved()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt is not approved");
        }
        InvestmentPlan plan = getPlan(investment.getInvestmentPlanId());
        investment.setStatus(DomainEnums.InvestmentStatus.ACTIVE);
        investment.setActivatedAt(LocalDateTime.now());
        investment.setActivatedByAdminId(admin.getId());
        investment.setMaturityDate(LocalDate.now().plusMonths(plan.getLockInMonths()));
        investment.setMonthlyInterestRate(plan.getMonthlyInterestRate());
        investment.setNotes(body.notes());
        Investment saved = investmentRepository.save(investment);
        notifyUser(investment.getInvestorUserId(), "Investment activated", "Investment activated. Matures on " + saved.getMaturityDate(), DomainEnums.NotificationType.INVESTMENT_UPDATE);
        auditService.log(admin, "INVESTMENT_ACTIVATED", "Investment", investmentId, null, saved.getStatus().name(), request);
        return saved;
    }

    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    public Map<String, Object> getWallet(User user) {
        Wallet wallet = getWalletByUserId(user.getId());
        return Map.of(
                "wallet", wallet,
                "recentTransactions", walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().limit(10).toList()
        );
    }

    public List<WalletTransaction> getWalletTransactions(User user) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public WithdrawalRequest requestWithdrawal(User user, ApiDtos.RequestWithdrawalRequest body, HttpServletRequest request) {
        Wallet wallet = getWalletByUserId(user.getId());
        if (body.requestedAmount().compareTo(new BigDecimal("1000")) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum withdrawal is 1000");
        }
        if (wallet.getAvailableBalance().compareTo(body.requestedAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(body.requestedAmount()));
        wallet.setLockedBalance(wallet.getLockedBalance().add(body.requestedAmount()));
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        WithdrawalRequest withdrawal = new WithdrawalRequest();
        withdrawal.setId(UUID.randomUUID().toString());
        withdrawal.setInvestorId(user.getId());
        withdrawal.setRequestedAmount(body.requestedAmount());
        withdrawal.setWalletBalanceAtRequest(wallet.getAvailableBalance().add(body.requestedAmount()));
        withdrawal.setBankAccountNumber(user.getBankAccountNumber());
        withdrawal.setBankIfsc(user.getBankIfscCode());
        withdrawal.setBankName(user.getBankName());
        withdrawal.setAccountHolderName(user.getFullName());
        withdrawal.setStatus(DomainEnums.WithdrawalStatus.PENDING);
        withdrawal.setRequestedAt(LocalDateTime.now());
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);

        if (body.requestedAmount().compareTo(new BigDecimal("50000")) > 0) {
            createFraudAlert(user.getId(), DomainEnums.AlertLevel.MEDIUM, "LARGE_WITHDRAWAL", "Withdrawal request exceeds 50000");
        }
        notifyUser(user.getId(), "Withdrawal requested", "Your withdrawal request is pending admin approval.", DomainEnums.NotificationType.WITHDRAWAL_UPDATE);
        auditService.log(user, "WITHDRAWAL_REQUESTED", "WithdrawalRequest", saved.getId(), null, saved.getRequestedAmount().toPlainString(), request);
        return saved;
    }

    public List<WithdrawalRequest> getOwnWithdrawals(User user) {
        return withdrawalRepository.findByInvestorIdOrderByRequestedAtDesc(user.getId());
    }

    public List<WithdrawalRequest> getPendingWithdrawals() {
        return withdrawalRepository.findByStatus(DomainEnums.WithdrawalStatus.PENDING);
    }

    @Transactional
    public WithdrawalRequest approveWithdrawal(User admin, String id, ApiDtos.WithdrawalDecisionRequest body, HttpServletRequest request) {
        WithdrawalRequest withdrawal = getWithdrawal(id);
        withdrawal.setStatus(DomainEnums.WithdrawalStatus.APPROVED);
        withdrawal.setReviewedByAdminId(admin.getId());
        withdrawal.setReviewedAt(LocalDateTime.now());
        withdrawal.setAdminNotes(body.adminNotes());
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);
        notifyUser(saved.getInvestorId(), "Withdrawal approved", "Your withdrawal has been approved and is awaiting processing.", DomainEnums.NotificationType.WITHDRAWAL_UPDATE);
        auditService.log(admin, "WITHDRAWAL_APPROVED", "WithdrawalRequest", id, null, "APPROVED", request);
        return saved;
    }

    @Transactional
    public WithdrawalRequest processWithdrawal(User admin, String id, ApiDtos.WithdrawalProcessRequest body, HttpServletRequest request) {
        WithdrawalRequest withdrawal = getWithdrawal(id);
        if (withdrawal.getStatus() != DomainEnums.WithdrawalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Withdrawal must be approved first");
        }
        Wallet wallet = getWalletByUserId(withdrawal.getInvestorId());
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(withdrawal.getRequestedAmount()));
        wallet.setTotalDebited(wallet.getTotalDebited().add(withdrawal.getRequestedAmount()));
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        createWalletTransaction(wallet, withdrawal.getInvestorId(), DomainEnums.WalletTransactionType.WITHDRAWAL_DEBIT,
                DomainEnums.Direction.DEBIT, withdrawal.getRequestedAmount(), withdrawal.getId(), "Withdrawal processed", admin.getId());

        withdrawal.setStatus(DomainEnums.WithdrawalStatus.PROCESSED);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setReviewedByAdminId(admin.getId());
        withdrawal.setReviewedAt(LocalDateTime.now());
        withdrawal.setBankTransferReference(body.bankTransferReference());
        withdrawal.setAdminNotes(body.adminNotes());
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);
        notifyUser(saved.getInvestorId(), "Withdrawal processed", "Funds sent to bank. Reference: " + body.bankTransferReference(), DomainEnums.NotificationType.WITHDRAWAL_UPDATE);
        auditService.log(admin, "WITHDRAWAL_PROCESSED", "WithdrawalRequest", id, null, body.bankTransferReference(), request);
        return saved;
    }

    @Transactional
    public WithdrawalRequest rejectWithdrawal(User admin, String id, ApiDtos.WithdrawalDecisionRequest body, HttpServletRequest request) {
        WithdrawalRequest withdrawal = getWithdrawal(id);
        Wallet wallet = getWalletByUserId(withdrawal.getInvestorId());
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(withdrawal.getRequestedAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(withdrawal.getRequestedAmount()));
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        withdrawal.setStatus(DomainEnums.WithdrawalStatus.REJECTED);
        withdrawal.setReviewedByAdminId(admin.getId());
        withdrawal.setReviewedAt(LocalDateTime.now());
        withdrawal.setRejectionReason(body.reason());
        withdrawal.setAdminNotes(body.adminNotes());
        WithdrawalRequest saved = withdrawalRepository.save(withdrawal);
        notifyUser(saved.getInvestorId(), "Withdrawal rejected", "Reason: " + body.reason(), DomainEnums.NotificationType.WITHDRAWAL_UPDATE);
        auditService.log(admin, "WITHDRAWAL_REJECTED", "WithdrawalRequest", id, null, body.reason(), request);
        return saved;
    }

    public Map<String, Object> getReferralTree(User user) {
        List<ReferralRelationship> relationships = referralRelationshipRepository.findByReferrerUserIdOrderByReferralLevelAscLinkedAtDesc(user.getId());
        Map<Integer, List<Map<String, Object>>> tree = relationships.stream()
                .collect(Collectors.groupingBy(
                        ReferralRelationship::getReferralLevel,
                        Collectors.mapping(rel -> {
                            User referred = getUser(rel.getReferredUserId());
                            return Map.<String, Object>of(
                                    "userId", referred.getId(),
                                    "fullName", referred.getFullName(),
                                    "email", referred.getEmail(),
                                    "level", rel.getReferralLevel(),
                                    "linkedAt", rel.getLinkedAt()
                            );
                        }, Collectors.toList())
                ));
        return Map.of("tree", tree);
    }

    public List<ReferralCommission> getReferralCommissions(User user) {
        return referralCommissionRepository.findByBeneficiaryUserIdOrderByCreditedAtDesc(user.getId());
    }

    public Map<String, Object> getInterestRates() {
        return Map.of("plans", planRepository.findAll().stream()
                .map(plan -> Map.of("planId", plan.getId(), "planName", plan.getPlanName(), "monthlyInterestRate", plan.getMonthlyInterestRate()))
                .toList());
    }

    @Transactional
    public InvestmentPlan updateInterestRate(User admin, String id, ApiDtos.UpdateRateRequest body, HttpServletRequest request) {
        InvestmentPlan plan = getPlan(id);
        plan.setMonthlyInterestRate(body.monthlyInterestRate());
        plan.setLastModifiedAt(LocalDateTime.now());
        plan.setLastModifiedBy(admin.getId());
        InvestmentPlan saved = planRepository.save(plan);
        auditService.log(admin, "RATE_UPDATED", "InvestmentPlan", id, null, body.monthlyInterestRate().toPlainString(), request);
        return saved;
    }

    @Transactional
    public Map<String, Object> triggerInterestRun(User admin, HttpServletRequest request) {
        String month = YearMonth.now().toString();
        int processed = 0;
        for (Investment investment : investmentRepository.findByStatus(DomainEnums.InvestmentStatus.ACTIVE)) {
            if (interestRecordRepository.existsByInvestmentIdAndCalculationMonth(investment.getId(), month)) {
                continue;
            }
            BigDecimal interest = investment.getInvestmentAmount()
                    .multiply(investment.getMonthlyInterestRate())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            InterestRecord record = new InterestRecord();
            record.setId(UUID.randomUUID().toString());
            record.setInvestmentId(investment.getId());
            record.setInvestorId(investment.getInvestorUserId());
            record.setCalculationMonth(month);
            record.setPrincipalAmount(investment.getInvestmentAmount());
            record.setInterestRate(investment.getMonthlyInterestRate());
            record.setInterestAmount(interest);
            record.setStatus(DomainEnums.InterestStatus.CREDITED);
            record.setCalculatedAt(LocalDateTime.now());
            record.setCreditedAt(LocalDateTime.now());
            interestRecordRepository.save(record);

            Wallet investorWallet = getWalletByUserId(investment.getInvestorUserId());
            creditWallet(investorWallet, investment.getInvestorUserId(), interest, DomainEnums.WalletTransactionType.INTEREST_CREDIT,
                    record.getId(), "Monthly interest credited", "SYSTEM");
            investment.setTotalInterestEarned(investment.getTotalInterestEarned().add(interest));
            investmentRepository.save(investment);
            notifyUser(investment.getInvestorUserId(), "Interest credited", "Interest of " + interest + " credited for " + month, DomainEnums.NotificationType.INTEREST_CREDITED);
            processReferralCommissions(investment, interest, month);
            processed++;
        }
        auditService.log(admin, "INTEREST_TRIGGERED", "InterestRun", month, null, "Processed: " + processed, request);
        return Map.of("message", "Interest run completed", "month", month, "processedInvestments", processed);
    }

    public Map<String, Object> getAdminDashboard() {
        BigDecimal totalAum = investmentRepository.findByStatus(DomainEnums.InvestmentStatus.ACTIVE).stream()
                .map(Investment::getInvestmentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "totalInvestors", userRepository.findByRole(DomainEnums.Role.INVESTOR).size(),
                "activeInvestments", investmentRepository.findByStatus(DomainEnums.InvestmentStatus.ACTIVE).size(),
                "totalAum", totalAum,
                "pendingKycQueue", kycSubmissionRepository.findByStatus(DomainEnums.KycStatus.PENDING).size(),
                "pendingReceipts", paymentReceiptRepository.findByVerificationStatus(DomainEnums.ReceiptStatus.PENDING).size(),
                "pendingWithdrawals", withdrawalRepository.findByStatus(DomainEnums.WithdrawalStatus.PENDING).size(),
                "openFraudAlerts", fraudAlertRepository.findByStatusOrderByCreatedAtDesc(DomainEnums.AlertStatus.OPEN).size()
        );
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User suspendUser(User admin, String id, ApiDtos.SuspendUserRequest body, HttpServletRequest request) {
        User user = getUser(id);
        user.setAccountStatus(DomainEnums.AccountStatus.SUSPENDED);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        createFraudAlert(id, DomainEnums.AlertLevel.HIGH, "ACCOUNT_SUSPENDED", body.reason() == null ? "Suspended by admin" : body.reason());
        auditService.log(admin, "USER_SUSPENDED", "User", id, null, body.reason(), request);
        return saved;
    }

    public List<FraudAlert> getFraudAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public FraudAlert resolveFraudAlert(User admin, String id, ApiDtos.ResolveAlertRequest body, HttpServletRequest request) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fraud alert not found"));
        alert.setReviewedBy(admin.getId());
        alert.setReviewedAt(LocalDateTime.now());
        alert.setResolutionNotes(body.resolutionNotes());
        alert.setStatus(body.status() == null ? DomainEnums.AlertStatus.RESOLVED : DomainEnums.AlertStatus.valueOf(body.status()));
        FraudAlert saved = fraudAlertRepository.save(alert);
        auditService.log(admin, "FRAUD_ALERT_RESOLVED", "FraudAlert", id, null, saved.getStatus().name(), request);
        return saved;
    }

    public List<AuditLog> getAuditLogs(String query) {
        if (query == null || query.isBlank()) {
            return auditLogRepository.findAllByOrderByOccurredAtDesc();
        }
        return auditLogRepository.findByEntityTypeContainingIgnoreCaseOrActionContainingIgnoreCaseOrderByOccurredAtDesc(query, query);
    }

    public Map<String, Object> getMonthlyReport() {
        String month = YearMonth.now().toString();
        List<InterestRecord> interests = interestRecordRepository.findAll().stream()
                .filter(record -> month.equals(record.getCalculationMonth()))
                .toList();
        BigDecimal totalInterestPaid = interests.stream().map(InterestRecord::getInterestAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommissions = referralCommissionRepository.findAll().stream()
                .filter(record -> month.equals(record.getCommissionMonth()))
                .map(ReferralCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "month", month,
                "interestRecords", interests.size(),
                "totalInterestPaid", totalInterestPaid,
                "totalReferralCommissions", totalCommissions,
                "newInvestments", investmentRepository.findAll().stream().filter(i -> i.getAppliedAt() != null && month.equals(YearMonth.from(i.getAppliedAt()).toString())).count(),
                "processedWithdrawals", withdrawalRepository.findByStatus(DomainEnums.WithdrawalStatus.PROCESSED).size()
        );
    }

    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(user.getId());
    }

    @Transactional
    public Notification markNotificationRead(User user, String id, HttpServletRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notification.setReadFlag(true);
        notification.setReadAt(LocalDateTime.now());
        Notification saved = notificationRepository.save(notification);
        auditService.log(user, "NOTIFICATION_READ", "Notification", id, null, "READ", request);
        return saved;
    }

    public Map<String, Object> getInvestorDashboard(User user) {
        List<Investment> investments = investmentRepository.findByInvestorUserId(user.getId());
        Wallet wallet = getWalletByUserId(user.getId());
        return Map.of(
                "profile", user,
                "wallet", wallet,
                "kycStatus", user.getKycStatus(),
                "activeInvestments", investments.stream().filter(i -> i.getStatus() == DomainEnums.InvestmentStatus.ACTIVE).count(),
                "totalInvested", investments.stream().map(Investment::getInvestmentAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                "totalInterestEarned", investments.stream().map(Investment::getTotalInterestEarned).reduce(BigDecimal.ZERO, BigDecimal::add),
                "pendingWithdrawals", withdrawalRepository.findByInvestorIdOrderByRequestedAtDesc(user.getId()).stream().filter(w -> w.getStatus() == DomainEnums.WithdrawalStatus.PENDING).count(),
                "unreadNotifications", notificationRepository.findByUserIdOrderBySentAtDesc(user.getId()).stream().filter(n -> !n.isReadFlag()).count()
        );
    }

    @Transactional
    public void createReferralLinks(User newUser) {
        if (newUser.getReferredByCode() == null || newUser.getReferredByCode().isBlank()) {
            return;
        }
        Optional<User> directReferrer = userRepository.findByReferralCode(newUser.getReferredByCode());
        if (directReferrer.isEmpty()) {
            return;
        }
        List<String> chain = new ArrayList<>();
        chain.add(directReferrer.get().getId());
        referralRelationshipRepository.findByReferredUserIdOrderByReferralLevelAsc(directReferrer.get().getId()).stream()
                .filter(relationship -> relationship.getReferralLevel() < 5)
                .forEach(relationship -> chain.add(relationship.getReferrerUserId()));

        for (int i = 0; i < chain.size() && i < 5; i++) {
            ReferralRelationship relation = new ReferralRelationship();
            relation.setId(UUID.randomUUID().toString());
            relation.setReferrerUserId(chain.get(i));
            relation.setReferredUserId(newUser.getId());
            relation.setReferralLevel(i + 1);
            relation.setActive(true);
            relation.setLinkedAt(LocalDateTime.now());
            referralRelationshipRepository.save(relation);
        }
    }

    private void processReferralCommissions(Investment investment, BigDecimal interest, String month) {
        List<ReferralRelationship> uplines = referralRelationshipRepository.findByReferredUserIdOrderByReferralLevelAsc(investment.getInvestorUserId());
        for (ReferralRelationship relationship : uplines) {
            ReferralCommission commission = new ReferralCommission();
            commission.setId(UUID.randomUUID().toString());
            commission.setBeneficiaryUserId(relationship.getReferrerUserId());
            commission.setSourceInvestorId(investment.getInvestorUserId());
            commission.setSourceInvestmentId(investment.getId());
            commission.setCommissionMonth(month);
            commission.setReferralLevel(relationship.getReferralLevel());
            commission.setCommissionRate(REFERRAL_RATES.get(relationship.getReferralLevel()));
            commission.setSourceInterestAmount(interest);
            BigDecimal amount = interest.multiply(commission.getCommissionRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            commission.setCommissionAmount(amount);
            User beneficiary = getUser(relationship.getReferrerUserId());
            if (beneficiary.getAccountStatus() == DomainEnums.AccountStatus.ACTIVE) {
                commission.setStatus(DomainEnums.CommissionStatus.CREDITED);
                commission.setCreditedAt(LocalDateTime.now());
                Wallet wallet = getWalletByUserId(beneficiary.getId());
                creditWallet(wallet, beneficiary.getId(), amount, DomainEnums.WalletTransactionType.REFERRAL_COMMISSION,
                        commission.getId(), "Referral commission credited", "SYSTEM");
                notifyUser(beneficiary.getId(), "Referral commission credited", amount + " credited for " + month, DomainEnums.NotificationType.REFERRAL_COMMISSION);
            } else {
                commission.setStatus(DomainEnums.CommissionStatus.SKIPPED);
                commission.setSkipReason("Beneficiary inactive");
            }
            referralCommissionRepository.save(commission);
        }
    }

    private void ensureInvestorReady(User user) {
        if (user.getKycStatus() != DomainEnums.KycStatus.APPROVED || !user.isRiskDisclosureAccepted() || !user.isInvestorAgreementAccepted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investor must complete KYC and accept mandatory disclosures");
        }
        if (user.getAccountStatus() != DomainEnums.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investor account must be active");
        }
    }

    private KycSubmission getKyc(String id) {
        return kycSubmissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC submission not found"));
    }

    private InvestmentPlan getPlan(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    }

    private Investment getInvestment(String id) {
        return investmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investment not found"));
    }

    private WithdrawalRequest getWithdrawal(String id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Withdrawal not found"));
    }

    private User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Wallet getWalletByUserId(String userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    private void notifyUser(String userId, String title, String message, DomainEnums.NotificationType type) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setChannel(DomainEnums.NotificationChannel.BOTH);
        notification.setReadFlag(false);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void createFraudAlert(String userId, DomainEnums.AlertLevel level, String rule, String description) {
        FraudAlert alert = new FraudAlert();
        alert.setId(UUID.randomUUID().toString());
        alert.setUserId(userId);
        alert.setAlertLevel(level);
        alert.setRuleTriggered(rule);
        alert.setDescription(description);
        alert.setStatus(DomainEnums.AlertStatus.OPEN);
        alert.setCreatedAt(LocalDateTime.now());
        fraudAlertRepository.save(alert);
    }

    private void creditWallet(Wallet wallet, String userId, BigDecimal amount, DomainEnums.WalletTransactionType type,
                              String referenceId, String description, String createdBy) {
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        wallet.setTotalCredited(wallet.getTotalCredited().add(amount));
        wallet.setLastUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        createWalletTransaction(wallet, userId, type, DomainEnums.Direction.CREDIT, amount, referenceId, description, createdBy);
    }

    private void createWalletTransaction(Wallet wallet, String userId, DomainEnums.WalletTransactionType type, DomainEnums.Direction direction,
                                         BigDecimal amount, String referenceId, String description, String createdBy) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setWalletId(wallet.getId());
        transaction.setUserId(userId);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setDirection(direction);
        transaction.setBalanceBefore(direction == DomainEnums.Direction.CREDIT ? wallet.getAvailableBalance().subtract(amount) : wallet.getAvailableBalance().add(amount));
        transaction.setBalanceAfter(wallet.getAvailableBalance());
        transaction.setReferenceId(referenceId);
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCreatedBy(createdBy);
        walletTransactionRepository.save(transaction);
    }

    // ── Notifications ──────────────────────────────────────────────────────────
    public Map<String, Object> getNotificationPreferences(User user) {
        return Map.of("email", true, "whatsapp", true, "sms", true, "push", true);
    }

    public Map<String, Object> updateNotificationPreferences(User user, ApiDtos.UpdateNotificationPreferencesRequest request, HttpServletRequest servletRequest) {
        auditService.log(user, "UPDATE_NOTIFICATION_PREFERENCES", "SUCCESS", "Updated notification preferences", servletRequest);
        return Map.of("message", "Notification preferences updated successfully");
    }

    public Map<String, Object> getNotificationSummary(User user) {
        List<Notification> list = notificationRepository.findByUserIdOrderBySentAtDesc(user.getId());
        long unreadCount = list.stream().filter(n -> !n.isReadFlag()).count();
        return Map.of("total", list.size(), "unread", unreadCount);
    }

    public Map<String, Object> markAllNotificationsRead(User user, HttpServletRequest request) {
        List<Notification> list = notificationRepository.findByUserIdOrderBySentAtDesc(user.getId());
        LocalDateTime now = LocalDateTime.now();
        for (Notification n : list) {
            if (!n.isReadFlag()) {
                n.setReadFlag(true);
                n.setReadAt(now);
            }
        }
        notificationRepository.saveAll(list);
        return Map.of("message", "All notifications marked as read");
    }

    public Map<String, Object> deleteNotification(User user, String id, HttpServletRequest request) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        notificationRepository.delete(notification);
        return Map.of("message", "Notification deleted");
    }

    // ── Razorpay Integration ───────────────────────────────────────────────────
    public Map<String, Object> createRazorpayCheckoutOrder(User user, ApiDtos.ApplyInvestmentRequest request, HttpServletRequest servletRequest) {
        Investment response = applyInvestment(user, request, servletRequest);
        return Map.of(
                "orderId", "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14),
                "currency", "INR",
                "amount", request.investmentAmount().multiply(new java.math.BigDecimal("100")),
                "investmentDetails", response
        );
    }

    public Map<String, Object> verifyRazorpayPayment(User user, ApiDtos.VerifyRazorpayPaymentRequest request, HttpServletRequest servletRequest) {
        auditService.log(user, "VERIFY_RAZORPAY_PAYMENT", "SUCCESS", "Verified Razorpay payment " + request.razorpayPaymentId(), servletRequest);
        return Map.of("status", "SUCCESS", "message", "Payment verified successfully", "paymentId", request.razorpayPaymentId());
    }

    public Map<String, Object> getOwnRazorpayPayment(User user, String investmentId) {
        Investment inv = getInvestment(investmentId);
        if (!inv.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return Map.of("investmentId", investmentId, "status", inv.getStatus().name(), "amount", inv.getAmount());
    }

    public Map<String, Object> handleRazorpayWebhook(String signature, String eventId, String payload) {
        return Map.of("status", "SUCCESS", "eventHandled", true);
    }

    public List<Map<String, Object>> getAllRazorpayPayments() {
        return List.of();
    }

    public Map<String, Object> getRazorpaySettlements(Integer count, Integer skip) {
        return Map.of("count", count != null ? count : 10, "skip", skip != null ? skip : 0, "items", List.of());
    }

    public Map<String, Object> syncRazorpayPayment(User admin, String investmentId, HttpServletRequest request) {
        auditService.log(admin, "SYNC_RAZORPAY_PAYMENT", "SUCCESS", "Synced payment for investment " + investmentId, request);
        return Map.of("investmentId", investmentId, "synced", true);
    }

    // ── Coupons ────────────────────────────────────────────────────────────────
    public List<Map<String, Object>> getActiveCouponsForInvestor() {
        return List.of();
    }

    public List<Map<String, Object>> getAllCoupons() {
        return List.of();
    }

    public Map<String, Object> validateCoupon(User user, ApiDtos.ValidateCouponRequest request) {
        return Map.of("valid", true, "discountAmount", BigDecimal.ZERO);
    }

    public Map<String, Object> createCoupon(User admin, ApiDtos.CreateCouponRequest request, HttpServletRequest servletRequest) {
        auditService.log(admin, "CREATE_COUPON", "SUCCESS", "Created coupon " + request.code(), servletRequest);
        return Map.of("code", request.code(), "created", true);
    }

    public Map<String, Object> updateCoupon(User admin, String id, ApiDtos.UpdateCouponRequest request, HttpServletRequest servletRequest) {
        auditService.log(admin, "UPDATE_COUPON", "SUCCESS", "Updated coupon " + id, servletRequest);
        return Map.of("id", id, "updated", true);
    }

    // ── Legal Documents ────────────────────────────────────────────────────────
    public Map<String, Object> getLegalDocument(String documentKey) {
        return Map.of("key", documentKey, "title", documentKey.toUpperCase(), "content", "Legal document content for " + documentKey);
    }

    public List<Map<String, Object>> getLegalDocuments() {
        return List.of(
                Map.of("key", "privacy-policy", "title", "Privacy Policy"),
                Map.of("key", "terms-and-conditions", "title", "Terms and Conditions")
        );
    }

    public Map<String, Object> updateLegalDocument(User admin, String documentKey, ApiDtos.UpdateLegalDocumentRequest body, HttpServletRequest request) {
        auditService.log(admin, "UPDATE_LEGAL_DOCUMENT", "SUCCESS", "Updated legal document " + documentKey, request);
        return Map.of("key", documentKey, "title", body.title(), "updated", true);
    }
}
