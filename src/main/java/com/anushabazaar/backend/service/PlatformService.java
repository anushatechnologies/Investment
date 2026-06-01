package com.anushabazaar.backend.service;

import com.anushabazaar.backend.domain.*;
import com.anushabazaar.backend.dto.ApiDtos;
import com.anushabazaar.backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlatformService {
    private static final String INVESTMENT_ACTIVATION_COMMISSION_MONTH = "INVESTMENT_ACTIVATION";

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
    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final ReferralRelationshipRepository referralRelationshipRepository;
    private final ReferralCommissionRepository referralCommissionRepository;
    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository couponRedemptionRepository;
    private final PlatformSettingRepository platformSettingRepository;
    private final InterestRecordRepository interestRecordRepository;
    private final NotificationRepository notificationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final AuditService auditService;
    private final RazorpayGatewayService razorpayGatewayService;
    private final ObjectMapper objectMapper;

    public PlatformService(UserRepository userRepository,
                           KycSubmissionRepository kycSubmissionRepository,
                           InvestmentPlanRepository planRepository,
                           InvestmentRepository investmentRepository,
                           PaymentReceiptRepository paymentReceiptRepository,
                           RazorpayPaymentRepository razorpayPaymentRepository,
                           WalletRepository walletRepository,
                           WalletTransactionRepository walletTransactionRepository,
                           WithdrawalRequestRepository withdrawalRepository,
                           ReferralRelationshipRepository referralRelationshipRepository,
                           ReferralCommissionRepository referralCommissionRepository,
                           CouponRepository couponRepository,
                           CouponRedemptionRepository couponRedemptionRepository,
                           PlatformSettingRepository platformSettingRepository,
                           InterestRecordRepository interestRecordRepository,
                           NotificationRepository notificationRepository,
                           SupportTicketRepository supportTicketRepository,
                           FraudAlertRepository fraudAlertRepository,
                           AuditLogRepository auditLogRepository,
                           StorageService storageService,
                           AuditService auditService,
                           RazorpayGatewayService razorpayGatewayService,
                           ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.kycSubmissionRepository = kycSubmissionRepository;
        this.planRepository = planRepository;
        this.investmentRepository = investmentRepository;
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.razorpayPaymentRepository = razorpayPaymentRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.referralRelationshipRepository = referralRelationshipRepository;
        this.referralCommissionRepository = referralCommissionRepository;
        this.couponRepository = couponRepository;
        this.couponRedemptionRepository = couponRedemptionRepository;
        this.platformSettingRepository = platformSettingRepository;
        this.interestRecordRepository = interestRecordRepository;
        this.notificationRepository = notificationRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService;
        this.auditService = auditService;
        this.razorpayGatewayService = razorpayGatewayService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KycSubmission submitKyc(User user, MultipartFile panCard, MultipartFile aadhaarFront, MultipartFile aadhaarBack,
                                   MultipartFile selfiePhoto, MultipartFile bankProof, String panNumber,
                                   String aadhaarLast4, LocalDate dateOfBirth, String address, HttpServletRequest request) {
        completeMissingKycProfile(user, panNumber, aadhaarLast4, dateOfBirth, address);
        KycSubmission kyc = latestOrNewKyc(user);
        boolean initialSubmission = isInitialKycSubmission(kyc);
        validateKycSubmissionFiles(kyc, initialSubmission, panCard, aadhaarFront, aadhaarBack, selfiePhoto, bankProof);

        updateKycDocument(
                kyc,
                panCard,
                kyc.getPanCardPath(),
                kyc.getPanCardStatus(),
                "PAN card image",
                savedPath -> kyc.setPanCardPath(savedPath),
                () -> kyc.setPanCardStatus(DomainEnums.DocumentReviewStatus.PENDING),
                () -> kyc.setPanCardRejectionReason(null)
        );
        updateKycDocument(
                kyc,
                aadhaarFront,
                kyc.getAadhaarFrontPath(),
                kyc.getAadhaarFrontStatus(),
                "Aadhaar front image",
                savedPath -> kyc.setAadhaarFrontPath(savedPath),
                () -> kyc.setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus.PENDING),
                () -> kyc.setAadhaarFrontRejectionReason(null)
        );
        updateKycDocument(
                kyc,
                aadhaarBack,
                kyc.getAadhaarBackPath(),
                kyc.getAadhaarBackStatus(),
                "Aadhaar back image",
                savedPath -> kyc.setAadhaarBackPath(savedPath),
                () -> kyc.setAadhaarBackStatus(DomainEnums.DocumentReviewStatus.PENDING),
                () -> kyc.setAadhaarBackRejectionReason(null)
        );
        updateKycDocument(
                kyc,
                selfiePhoto,
                kyc.getSelfiePath(),
                kyc.getSelfieStatus(),
                "Selfie/profile photo",
                savedPath -> kyc.setSelfiePath(savedPath),
                () -> kyc.setSelfieStatus(DomainEnums.DocumentReviewStatus.PENDING),
                () -> kyc.setSelfieRejectionReason(null)
        );
        updateKycDocument(
                kyc,
                bankProof,
                kyc.getBankProofPath(),
                kyc.getBankProofStatus(),
                "Bank passbook or statement",
                savedPath -> kyc.setBankProofPath(savedPath),
                () -> kyc.setBankProofStatus(DomainEnums.DocumentReviewStatus.PENDING),
                () -> kyc.setBankProofRejectionReason(null)
        );
        kyc.setStatus(DomainEnums.KycStatus.PENDING);
        kyc.setSubmittedAt(LocalDateTime.now());
        kyc.setReviewedByAdminId(null);
        kyc.setReviewedAt(null);
        kyc.setRejectionReason(null);
        kyc.setAdminNotes(null);
        user.setKycStatus(DomainEnums.KycStatus.PENDING);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.KYC_PENDING);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(
                user.getId(),
                initialSubmission ? "KYC submitted" : "KYC re-submitted",
                initialSubmission ? "Your KYC documents are under review." : "Your updated KYC documents are under review.",
                DomainEnums.NotificationType.KYC_UPDATE
        );
        auditService.log(user, initialSubmission ? "KYC_SUBMITTED" : "KYC_RESUBMITTED", "KycSubmission", saved.getId(), null, "PENDING", request);
        return saved;
    }

    @Transactional
    public KycSubmission submitPan(User user, MultipartFile panCard, String panNumber, HttpServletRequest request) {
        requireFile(panCard, "PAN card image");
        if (isBlank(panNumber)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PAN number is required");
        }
        KycSubmission kyc = latestOrNewKyc(user);
        kyc.setPanCardPath(storageService.save(panCard, "kyc"));
        kyc.setPanCardStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setPanCardRejectionReason(null);
        user.setPanNumber(panNumber);
        return saveKycProgress(user, kyc, "KYC_PAN_UPLOADED", "PAN uploaded for review", request);
    }

    @Transactional
    public KycSubmission submitAadhaar(User user, MultipartFile aadhaarFront, MultipartFile aadhaarBack,
                                       String aadhaarNumber, String aadhaarLast4, String address,
                                       HttpServletRequest request) {
        requireFile(aadhaarFront, "Aadhaar front image");
        requireFile(aadhaarBack, "Aadhaar back image");
        String last4 = !isBlank(aadhaarLast4) ? aadhaarLast4 : aadhaarNumber == null || aadhaarNumber.length() < 4
                ? null
                : aadhaarNumber.substring(aadhaarNumber.length() - 4);
        if (isBlank(last4) || !last4.matches("\\d{4}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aadhaar number or last 4 digits are required");
        }
        KycSubmission kyc = latestOrNewKyc(user);
        kyc.setAadhaarFrontPath(storageService.save(aadhaarFront, "kyc"));
        kyc.setAadhaarBackPath(storageService.save(aadhaarBack, "kyc"));
        kyc.setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setAadhaarBackStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setAadhaarFrontRejectionReason(null);
        kyc.setAadhaarBackRejectionReason(null);
        user.setAadhaarLast4(last4);
        if (!isBlank(address)) {
            user.setAddress(address);
        }
        return saveKycProgress(user, kyc, "KYC_AADHAAR_UPLOADED", "Aadhaar uploaded for review", request);
    }

    @Transactional
    public KycSubmission uploadSelfie(User user, MultipartFile selfiePhoto, HttpServletRequest request) {
        requireFile(selfiePhoto, "Selfie/profile photo");
        KycSubmission kyc = latestOrNewKyc(user);
        kyc.setSelfiePath(storageService.save(selfiePhoto, "kyc"));
        kyc.setSelfieStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setSelfieRejectionReason(null);
        return saveKycProgress(user, kyc, "KYC_SELFIE_UPLOADED", "Selfie uploaded for review", request);
    }

    private void completeMissingKycProfile(User user, String panNumber, String aadhaarLast4, LocalDate dateOfBirth, String address) {
        if (isBlank(user.getPanNumber())) {
            if (isBlank(panNumber)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PAN number is required for KYC");
            }
            user.setPanNumber(panNumber);
        }
        if (isBlank(user.getAadhaarLast4())) {
            if (isBlank(aadhaarLast4) || !aadhaarLast4.matches("\\d{4}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aadhaar last 4 digits are required for KYC");
            }
            user.setAadhaarLast4(aadhaarLast4);
        }
        if (user.getDateOfBirth() == null) {
            if (dateOfBirth == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth is required for KYC");
            }
            if (dateOfBirth.isAfter(LocalDate.now().minusYears(18))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investor must be at least 18 years old");
            }
            user.setDateOfBirth(dateOfBirth);
        }
        if (isBlank(user.getAddress())) {
            if (isBlank(address)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address is required for KYC");
            }
            user.setAddress(address);
        }
    }

    private KycSubmission latestOrNewKyc(User user) {
        KycSubmission kyc = kycSubmissionRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getId()).orElseGet(KycSubmission::new);
        kyc.setId(kyc.getId() == null ? UUID.randomUUID().toString() : kyc.getId());
        kyc.setUserId(user.getId());
        return kyc;
    }

    private KycSubmission saveKycProgress(User user, KycSubmission kyc, String auditAction, String notificationMessage, HttpServletRequest request) {
        kyc.setStatus(DomainEnums.KycStatus.PENDING);
        kyc.setSubmittedAt(LocalDateTime.now());
        kyc.setReviewedByAdminId(null);
        kyc.setReviewedAt(null);
        kyc.setRejectionReason(null);
        user.setKycStatus(DomainEnums.KycStatus.PENDING);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.KYC_PENDING);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC updated", notificationMessage, DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(user, auditAction, "KycSubmission", saved.getId(), null, "PENDING", request);
        return saved;
    }

    private void markAllDocumentsPending(KycSubmission kyc) {
        kyc.setPanCardStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setAadhaarBackStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setSelfieStatus(DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setBankProofStatus(kyc.getBankProofPath() == null ? DomainEnums.DocumentReviewStatus.NOT_UPLOADED : DomainEnums.DocumentReviewStatus.PENDING);
        kyc.setPanCardRejectionReason(null);
        kyc.setAadhaarFrontRejectionReason(null);
        kyc.setAadhaarBackRejectionReason(null);
        kyc.setSelfieRejectionReason(null);
        kyc.setBankProofRejectionReason(null);
    }

    private boolean isInitialKycSubmission(KycSubmission kyc) {
        return isBlank(kyc.getPanCardPath())
                && isBlank(kyc.getAadhaarFrontPath())
                && isBlank(kyc.getAadhaarBackPath())
                && isBlank(kyc.getSelfiePath())
                && isBlank(kyc.getBankProofPath());
    }

    private void validateKycSubmissionFiles(KycSubmission kyc,
                                            boolean initialSubmission,
                                            MultipartFile panCard,
                                            MultipartFile aadhaarFront,
                                            MultipartFile aadhaarBack,
                                            MultipartFile selfiePhoto,
                                            MultipartFile bankProof) {
        if (initialSubmission) {
            requireFile(panCard, "PAN card image");
            requireFile(aadhaarFront, "Aadhaar front image");
            requireFile(aadhaarBack, "Aadhaar back image");
            requireFile(selfiePhoto, "Selfie/profile photo");
            requireFile(bankProof, "Bank passbook or statement");
            return;
        }

        requireDocumentIfNeeded(panCard, kyc.getPanCardPath(), kyc.getPanCardStatus(), "PAN card image");
        requireDocumentIfNeeded(aadhaarFront, kyc.getAadhaarFrontPath(), kyc.getAadhaarFrontStatus(), "Aadhaar front image");
        requireDocumentIfNeeded(aadhaarBack, kyc.getAadhaarBackPath(), kyc.getAadhaarBackStatus(), "Aadhaar back image");
        requireDocumentIfNeeded(selfiePhoto, kyc.getSelfiePath(), kyc.getSelfieStatus(), "Selfie/profile photo");
        requireDocumentIfNeeded(bankProof, kyc.getBankProofPath(), kyc.getBankProofStatus(), "Bank passbook or statement");

        boolean anyNewFile = !isEmptyFile(panCard)
                || !isEmptyFile(aadhaarFront)
                || !isEmptyFile(aadhaarBack)
                || !isEmptyFile(selfiePhoto)
                || !isEmptyFile(bankProof);
        if (!anyNewFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload at least one requested KYC document");
        }
    }

    private void requireDocumentIfNeeded(MultipartFile file,
                                         String currentPath,
                                         DomainEnums.DocumentReviewStatus status,
                                         String label) {
        if (isEmptyFile(file) && (isBlank(currentPath) || status == DomainEnums.DocumentReviewStatus.REUPLOAD_REQUIRED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
    }

    private void updateKycDocument(KycSubmission kyc,
                                   MultipartFile file,
                                   String currentPath,
                                   DomainEnums.DocumentReviewStatus currentStatus,
                                   String label,
                                   java.util.function.Consumer<String> pathSetter,
                                   Runnable statusSetter,
                                   Runnable rejectionReasonResetter) {
        if (isEmptyFile(file)) {
            if (isBlank(currentPath)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
            }
            return;
        }
        pathSetter.accept(storageService.save(file, "kyc"));
        statusSetter.run();
        rejectionReasonResetter.run();
    }

    private boolean isEmptyFile(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void requireFile(MultipartFile file, String label) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
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
        kyc.setPanCardStatus(DomainEnums.DocumentReviewStatus.APPROVED);
        kyc.setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus.APPROVED);
        kyc.setAadhaarBackStatus(DomainEnums.DocumentReviewStatus.APPROVED);
        kyc.setSelfieStatus(DomainEnums.DocumentReviewStatus.APPROVED);
        kyc.setBankProofStatus(kyc.getBankProofPath() == null ? DomainEnums.DocumentReviewStatus.NOT_UPLOADED : DomainEnums.DocumentReviewStatus.APPROVED);
        kyc.setReviewedByAdminId(admin.getId());
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setAdminNotes(notes);
        user.setKycStatus(DomainEnums.KycStatus.APPROVED);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.KYC_COMPLETED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC approved", "Your KYC has been approved. Link your bank account to continue.", DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(admin, "KYC_APPROVED", "KycSubmission", id, null, "APPROVED", request);
        return saved;
    }

    @Transactional
    public KycSubmission rejectKyc(User admin, String id, ApiDtos.KycDecisionRequest body, HttpServletRequest request) {
        KycSubmission kyc = getKyc(id);
        User user = getUser(kyc.getUserId());
        kyc.setStatus(DomainEnums.KycStatus.REJECTED);
        kyc.setPanCardStatus(DomainEnums.DocumentReviewStatus.REJECTED);
        kyc.setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus.REJECTED);
        kyc.setAadhaarBackStatus(DomainEnums.DocumentReviewStatus.REJECTED);
        kyc.setSelfieStatus(DomainEnums.DocumentReviewStatus.REJECTED);
        kyc.setBankProofStatus(kyc.getBankProofPath() == null ? DomainEnums.DocumentReviewStatus.NOT_UPLOADED : DomainEnums.DocumentReviewStatus.REJECTED);
        kyc.setReviewedByAdminId(admin.getId());
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setRejectionReason(body.reason());
        kyc.setAdminNotes(body.adminNotes());
        user.setKycStatus(DomainEnums.KycStatus.REJECTED);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.KYC_PENDING);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC rejected", "KYC rejected. Reason: " + body.reason(), DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(admin, "KYC_REJECTED", "KycSubmission", id, null, body.reason(), request);
        return saved;
    }

    @Transactional
    public KycSubmission rejectKycDocuments(User admin, String id, ApiDtos.KycDocumentRejectionRequest body, HttpServletRequest request) {
        KycSubmission kyc = getKyc(id);
        User user = getUser(kyc.getUserId());
        if (body.panCard()) {
            kyc.setPanCardStatus(DomainEnums.DocumentReviewStatus.REUPLOAD_REQUIRED);
            kyc.setPanCardRejectionReason(body.reason());
        }
        if (body.aadhaarFront()) {
            kyc.setAadhaarFrontStatus(DomainEnums.DocumentReviewStatus.REUPLOAD_REQUIRED);
            kyc.setAadhaarFrontRejectionReason(body.reason());
        }
        if (body.aadhaarBack()) {
            kyc.setAadhaarBackStatus(DomainEnums.DocumentReviewStatus.REUPLOAD_REQUIRED);
            kyc.setAadhaarBackRejectionReason(body.reason());
        }
        if (body.selfie()) {
            kyc.setSelfieStatus(DomainEnums.DocumentReviewStatus.REUPLOAD_REQUIRED);
            kyc.setSelfieRejectionReason(body.reason());
        }
        if (body.bankProof()) {
            kyc.setBankProofStatus(DomainEnums.DocumentReviewStatus.REUPLOAD_REQUIRED);
            kyc.setBankProofRejectionReason(body.reason());
        }
        kyc.setStatus(DomainEnums.KycStatus.REUPLOAD_REQUIRED);
        kyc.setReviewedByAdminId(admin.getId());
        kyc.setReviewedAt(LocalDateTime.now());
        kyc.setRejectionReason(body.reason());
        kyc.setAdminNotes(body.adminNotes());
        user.setKycStatus(DomainEnums.KycStatus.REUPLOAD_REQUIRED);
        user.setOnboardingStatus(DomainEnums.OnboardingStatus.KYC_PENDING);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        KycSubmission saved = kycSubmissionRepository.save(kyc);
        notifyUser(user.getId(), "KYC documents need reupload", "Reason: " + body.reason(), DomainEnums.NotificationType.KYC_UPDATE);
        auditService.log(admin, "KYC_DOCUMENTS_REUPLOAD_REQUIRED", "KycSubmission", id, null, body.reason(), request);
        return saved;
    }

    public Map<String, Object> getKycDocuments(String id) {
        KycSubmission kyc = getKyc(id);
        Map<String, Object> documents = new LinkedHashMap<>();
        documents.put("id", kyc.getId());
        documents.put("userId", kyc.getUserId());
        documents.put("panCard", kyc.getPanCardPath());
        documents.put("aadhaarFront", kyc.getAadhaarFrontPath());
        documents.put("aadhaarBack", kyc.getAadhaarBackPath());
        documents.put("selfie", kyc.getSelfiePath());
        documents.put("bankProof", kyc.getBankProofPath());
        documents.put("panCardStatus", kyc.getPanCardStatus());
        documents.put("aadhaarFrontStatus", kyc.getAadhaarFrontStatus());
        documents.put("aadhaarBackStatus", kyc.getAadhaarBackStatus());
        documents.put("selfieStatus", kyc.getSelfieStatus());
        documents.put("bankProofStatus", kyc.getBankProofStatus());
        documents.put("panCardRejectionReason", kyc.getPanCardRejectionReason());
        documents.put("aadhaarFrontRejectionReason", kyc.getAadhaarFrontRejectionReason());
        documents.put("aadhaarBackRejectionReason", kyc.getAadhaarBackRejectionReason());
        documents.put("selfieRejectionReason", kyc.getSelfieRejectionReason());
        documents.put("bankProofRejectionReason", kyc.getBankProofRejectionReason());
        documents.put("status", kyc.getStatus());
        documents.put("rejectionReason", kyc.getRejectionReason());
        documents.put("adminNotes", kyc.getAdminNotes());
        return documents;
    }

    public Map<String, Object> getKycDocumentsByUserId(String userId) {
        KycSubmission kyc = kycSubmissionRepository.findTopByUserIdOrderBySubmittedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("KYC submission not found for user"));
        return getKycDocuments(kyc.getId());
    }

    public List<InvestmentPlan> getActivePlans() {
        return planRepository.findByActiveTrue();
    }

    public List<InvestmentPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public List<Coupon> getActiveCouponsForInvestor() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findByStatusOrderByCreatedAtDesc(DomainEnums.CouponStatus.ACTIVE).stream()
                .filter(coupon -> coupon.getValidFrom() == null || !coupon.getValidFrom().isAfter(now))
                .filter(coupon -> coupon.getValidUntil() == null || !coupon.getValidUntil().isBefore(now))
                .toList();
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Map<String, Object> getReferralSettings() {
        Map<Integer, BigDecimal> rates = getReferralRates();
        return Map.of(
                "level1Rate", rates.getOrDefault(1, BigDecimal.ZERO),
                "level2Rate", rates.getOrDefault(2, BigDecimal.ZERO),
                "level3Rate", rates.getOrDefault(3, BigDecimal.ZERO),
                "level4Rate", rates.getOrDefault(4, BigDecimal.ZERO),
                "level5Rate", rates.getOrDefault(5, BigDecimal.ZERO)
        );
    }

    @Transactional
    public Coupon createCoupon(User admin, ApiDtos.CreateCouponRequest body, HttpServletRequest request) {
        Coupon coupon = new Coupon();
        coupon.setId(UUID.randomUUID().toString());
        coupon.setCode(body.code().trim().toUpperCase());
        if (couponRepository.findByCodeIgnoreCase(coupon.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code already exists");
        }
        applyCouponFields(coupon, body.title(), body.description(), body.type(), body.valueAmount(), body.minimumInvestmentAmount(),
                body.maximumCashbackAmount(), body.totalUsageLimit(), body.perUserUsageLimit(), body.firstInvestmentOnly(),
                body.validFrom(), body.validUntil(), body.status(), admin.getId(), true);
        Coupon saved = couponRepository.save(coupon);
        auditService.log(admin, "COUPON_CREATED", "Coupon", saved.getId(), null, saved.getCode(), request);
        return saved;
    }

    @Transactional
    public Coupon updateCoupon(User admin, String couponId, ApiDtos.UpdateCouponRequest body, HttpServletRequest request) {
        Coupon coupon = getCoupon(couponId);
        applyCouponFields(coupon, body.title(), body.description(), body.type(), body.valueAmount(), body.minimumInvestmentAmount(),
                body.maximumCashbackAmount(), body.totalUsageLimit(), body.perUserUsageLimit(), body.firstInvestmentOnly(),
                body.validFrom(), body.validUntil(), body.status(), admin.getId(), false);
        coupon.setTitle(body.title());
        Coupon saved = couponRepository.save(coupon);
        auditService.log(admin, "COUPON_UPDATED", "Coupon", saved.getId(), null, saved.getCode(), request);
        return saved;
    }

    @Transactional
    public Map<String, Object> updateReferralSettings(User admin, ApiDtos.UpdateReferralSettingsRequest body, HttpServletRequest request) {
        Map<Integer, BigDecimal> nextRates = Map.of(
                1, sanitizeReferralRate(body.level1Rate(), REFERRAL_RATES.get(1)),
                2, sanitizeReferralRate(body.level2Rate(), REFERRAL_RATES.get(2)),
                3, sanitizeReferralRate(body.level3Rate(), REFERRAL_RATES.get(3)),
                4, sanitizeReferralRate(body.level4Rate(), REFERRAL_RATES.get(4)),
                5, sanitizeReferralRate(body.level5Rate(), REFERRAL_RATES.get(5))
        );
        nextRates.forEach((level, rate) -> upsertSetting("referral.rate.level" + level, rate.toPlainString(), admin.getId()));
        auditService.log(admin, "REFERRAL_SETTINGS_UPDATED", "PlatformSetting", "referral-rates", null, nextRates.toString(), request);
        return getReferralSettings();
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
        Map<String, Object> couponPreview = resolveCouponPreview(user, body.investmentAmount(), body.couponCode(), false);
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
        investment.setAppliedCouponCode((String) couponPreview.get("couponCode"));
        investment.setCouponCashbackAmount((BigDecimal) couponPreview.get("cashbackAmount"));
        investment.setCouponCredited(false);
        Investment saved = investmentRepository.save(investment);
        reserveCouponRedemption(user, saved, couponPreview);
        notifyUser(user.getId(), "Investment applied", "Investment created. Upload payment receipt to continue.", DomainEnums.NotificationType.INVESTMENT_UPDATE);
        auditService.log(user, "INVESTMENT_APPLIED", "Investment", saved.getId(), null, body.investmentAmount().toPlainString(), request);
        return saved;
    }

    @Transactional
    public Map<String, Object> createRazorpayCheckoutOrder(User user, ApiDtos.ApplyInvestmentRequest body, HttpServletRequest request) {
        ensureInvestorReady(user);
        InvestmentPlan plan = getPlan(body.investmentPlanId());
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan is inactive");
        }
        if (body.investmentAmount().compareTo(plan.getMinimumAmount()) < 0 || body.investmentAmount().compareTo(plan.getMaximumAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Investment amount is outside plan limits");
        }
        Map<String, Object> couponPreview = resolveCouponPreview(user, body.investmentAmount(), body.couponCode(), false);

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
        investment.setAppliedCouponCode((String) couponPreview.get("couponCode"));
        investment.setCouponCashbackAmount((BigDecimal) couponPreview.get("cashbackAmount"));
        investment.setCouponCredited(false);

        Map<String, Object> notes = new LinkedHashMap<>();
        notes.put("investmentId", investment.getId());
        notes.put("investorId", user.getId());
        notes.put("planId", plan.getId());
        notes.put("email", user.getEmail());
        notes.put("mobileNumber", user.getMobileNumber());
        notes.put("couponCode", investment.getAppliedCouponCode());

        String receipt = "inv_" + investment.getId().replace("-", "").substring(0, 20);
        Map<String, Object> razorpayOrder = razorpayGatewayService.createOrder(
                body.investmentAmount(),
                razorpayGatewayService.properties().getCurrency(),
                receipt,
                notes
        );

        Investment savedInvestment = investmentRepository.save(investment);
        reserveCouponRedemption(user, savedInvestment, couponPreview);

        RazorpayPayment razorpayPayment = new RazorpayPayment();
        razorpayPayment.setId(UUID.randomUUID().toString());
        razorpayPayment.setInvestmentId(savedInvestment.getId());
        razorpayPayment.setInvestorId(user.getId());
        razorpayPayment.setRazorpayOrderId(asText(razorpayOrder.get("id")));
        razorpayPayment.setAmount(body.investmentAmount());
        razorpayPayment.setCurrency(asText(razorpayOrder.getOrDefault("currency", razorpayGatewayService.properties().getCurrency())));
        razorpayPayment.setStatus(asText(razorpayOrder.get("status")));
        razorpayPayment.setCaptured(false);
        razorpayPayment.setCheckoutOrderCreatedAt(LocalDateTime.now());
        razorpayPayment.setLastSyncedAt(LocalDateTime.now());
        razorpayPayment.setOrderPayload(writeJson(razorpayOrder));
        RazorpayPayment savedPayment = razorpayPaymentRepository.save(razorpayPayment);

        notifyUser(user.getId(), "Payment initiated", "Razorpay checkout order created for your investment.", DomainEnums.NotificationType.INVESTMENT_UPDATE);
        auditService.log(user, "RAZORPAY_ORDER_CREATED", "RazorpayPayment", savedPayment.getId(), null, savedPayment.getRazorpayOrderId(), request);

        Map<String, Object> checkout = new LinkedHashMap<>();
        checkout.put("keyId", razorpayGatewayService.properties().getKeyId());
        checkout.put("orderId", savedPayment.getRazorpayOrderId());
        checkout.put("amount", savedPayment.getAmount());
        checkout.put("currency", savedPayment.getCurrency());
        checkout.put("investorName", user.getFullName());
        checkout.put("investorEmail", user.getEmail());
        checkout.put("investorContact", user.getMobileNumber());
        checkout.put("planName", plan.getPlanName());
        checkout.put("description", "Investment for " + plan.getPlanName());
        checkout.put("couponCode", savedInvestment.getAppliedCouponCode());
        checkout.put("couponCashbackAmount", savedInvestment.getCouponCashbackAmount() == null ? BigDecimal.ZERO : savedInvestment.getCouponCashbackAmount());

        return Map.of(
                "investment", savedInvestment,
                "payment", savedPayment,
                "checkout", checkout
        );
    }

    @Transactional
    public Map<String, Object> verifyRazorpayPayment(User user, ApiDtos.VerifyRazorpayPaymentRequest body, HttpServletRequest request) {
        Investment investment = getOwnInvestment(user, body.investmentId());
        RazorpayPayment razorpayPayment = razorpayPaymentRepository.findByInvestmentId(investment.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Razorpay checkout order not found"));

        if (!Objects.equals(razorpayPayment.getRazorpayOrderId(), body.razorpayOrderId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay order mismatch");
        }
        if (!razorpayGatewayService.verifyCheckoutSignature(body.razorpayOrderId(), body.razorpayPaymentId(), body.razorpaySignature())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay signature");
        }

        razorpayPayment.setRazorpayPaymentId(body.razorpayPaymentId());
        razorpayPayment.setRazorpaySignature(body.razorpaySignature());
        razorpayPayment.setSignatureVerifiedAt(LocalDateTime.now());

        Map<String, Object> paymentPayload = refreshPaymentFromGateway(razorpayPayment);
        String status = asText(paymentPayload.get("status"));
        boolean captured = Boolean.TRUE.equals(paymentPayload.get("captured"));
        if ("authorized".equalsIgnoreCase(status) && !captured) {
            paymentPayload = razorpayGatewayService.capturePayment(body.razorpayPaymentId(), razorpayPayment.getAmount(), razorpayPayment.getCurrency());
            applyPaymentSnapshot(razorpayPayment, paymentPayload);
            status = asText(paymentPayload.get("status"));
            captured = Boolean.TRUE.equals(paymentPayload.get("captured"));
        }
        if (!captured && !"captured".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment is not captured yet");
        }

        finalizeCapturedInvestmentPayment(investment, razorpayPayment, paymentPayload, "CLIENT_VERIFY");

        auditService.log(user, "RAZORPAY_PAYMENT_VERIFIED", "RazorpayPayment", razorpayPayment.getId(), null, razorpayPayment.getRazorpayPaymentId(), request);
        return Map.of(
                "investment", investmentRepository.findById(investment.getId()).orElse(investment),
                "payment", razorpayPaymentRepository.findById(razorpayPayment.getId()).orElse(razorpayPayment),
                "message", "Payment verified and investment activated"
        );
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
        auditService.log(user, "RECEIPT_UPLOADED", "PaymentReceipt", saved.getId(), null, saved.getFileName(), request);
        return saved;
    }

    public List<Investment> getOwnInvestments(User user) {
        return investmentRepository.findByInvestorUserId(user.getId());
    }

    public RazorpayPayment getOwnRazorpayPayment(User user, String investmentId) {
        Investment investment = getOwnInvestment(user, investmentId);
        return razorpayPaymentRepository.findByInvestmentId(investment.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Razorpay payment not found"));
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
        cancelCouponRedemption(saved.getId());
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
        } else {
            receipt.setVerificationStatus(DomainEnums.ReceiptStatus.REJECTED);
            receipt.setVerifiedByAdminId(admin.getId());
            receipt.setVerifiedAt(LocalDateTime.now());
            receipt.setRejectionReason(body.rejectionReason());
            investment.setReceiptApproved(false);
            investment.setStatus(DomainEnums.InvestmentStatus.REJECTED);
            cancelCouponRedemption(investment.getId());
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
        Investment saved = activateInvestmentRecord(investment, admin.getId(), body.notes());
        notifyUser(investment.getInvestorUserId(), "Investment activated", "Investment activated. Matures on " + saved.getMaturityDate(), DomainEnums.NotificationType.INVESTMENT_UPDATE);
        auditService.log(admin, "INVESTMENT_ACTIVATED", "Investment", investmentId, null, saved.getStatus().name(), request);
        return saved;
    }

    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    public List<RazorpayPayment> getAllRazorpayPayments() {
        return razorpayPaymentRepository.findAllByOrderByCheckoutOrderCreatedAtDesc();
    }

    public Map<String, Object> getRazorpaySettlements(Integer count, Integer skip) {
        Map<String, Object> settlements = razorpayGatewayService.fetchSettlements(count, skip);
        return Map.of(
                "gateway", "RAZORPAY",
                "fetchedAt", LocalDateTime.now(),
                "settlements", settlements
        );
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
        List<Map<String, Object>> levels = tree.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<Map<String, Object>> members = entry.getValue();
                    BigDecimal income = referralCommissionRepository.findByBeneficiaryUserIdOrderByCreditedAtDesc(user.getId()).stream()
                            .filter(commission -> entry.getKey().equals(commission.getReferralLevel()))
                            .map(ReferralCommission::getCommissionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.<String, Object>of(
                            "level", "Level " + entry.getKey(),
                            "levelNumber", entry.getKey(),
                            "members", members.size(),
                            "count", members.size(),
                            "income", income,
                            "users", members
                    );
                })
                .toList();
        return Map.of("tree", tree, "levels", levels);
    }

    public List<ReferralCommission> getReferralCommissions(User user) {
        return referralCommissionRepository.findByBeneficiaryUserIdOrderByCreditedAtDesc(user.getId());
    }

    public Map<String, Object> validateCoupon(User user, ApiDtos.ValidateCouponRequest body) {
        return resolveCouponPreview(user, body.investmentAmount(), body.couponCode(), true);
    }

    public Map<String, Object> getAdminReferralReport() {
        List<ReferralRelationship> relationships = referralRelationshipRepository.findAll();
        List<ReferralCommission> commissions = referralCommissionRepository.findAll();
        Map<String, User> usersById = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        BigDecimal totalCommissions = commissions.stream()
                .map(ReferralCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditedCommissions = commissions.stream()
                .filter(commission -> commission.getStatus() == DomainEnums.CommissionStatus.CREDITED)
                .map(ReferralCommission::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long skippedCommissions = commissions.stream()
                .filter(commission -> commission.getStatus() == DomainEnums.CommissionStatus.SKIPPED)
                .count();

        List<Map<String, Object>> levelSummary = getReferralRates().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.<String, Object>of(
                        "level", entry.getKey(),
                        "rate", entry.getValue(),
                        "relationships", relationships.stream().filter(rel -> entry.getKey().equals(rel.getReferralLevel())).count(),
                        "commissionAmount", commissions.stream()
                                .filter(commission -> entry.getKey().equals(commission.getReferralLevel()))
                                .map(ReferralCommission::getCommissionAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .toList();

        Map<String, BigDecimal> commissionByBeneficiary = commissions.stream()
                .collect(Collectors.groupingBy(
                        ReferralCommission::getBeneficiaryUserId,
                        Collectors.mapping(ReferralCommission::getCommissionAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));
        Map<String, Long> referralsByBeneficiary = relationships.stream()
                .collect(Collectors.groupingBy(ReferralRelationship::getReferrerUserId, Collectors.counting()));

        List<Map<String, Object>> topReferrers = referralsByBeneficiary.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(10)
                .map(entry -> {
                    User referrer = usersById.get(entry.getKey());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("userId", entry.getKey());
                    row.put("name", referrer == null ? "Unknown user" : referrer.getFullName());
                    row.put("email", referrer == null ? null : referrer.getEmail());
                    row.put("referralCode", referrer == null ? null : referrer.getReferralCode());
                    row.put("referralCount", entry.getValue());
                    row.put("commissionAmount", commissionByBeneficiary.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                    row.put("accountStatus", referrer == null ? null : referrer.getAccountStatus());
                    return row;
                })
                .toList();

        List<Map<String, Object>> recentRelationships = relationships.stream()
                .sorted(Comparator.comparing(ReferralRelationship::getLinkedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(25)
                .map(relationship -> {
                    User referrer = usersById.get(relationship.getReferrerUserId());
                    User referred = usersById.get(relationship.getReferredUserId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", relationship.getId());
                    row.put("referrerUserId", relationship.getReferrerUserId());
                    row.put("referrerName", referrer == null ? "Unknown user" : referrer.getFullName());
                    row.put("referrerCode", referrer == null ? null : referrer.getReferralCode());
                    row.put("referredUserId", relationship.getReferredUserId());
                    row.put("referredName", referred == null ? "Unknown user" : referred.getFullName());
                    row.put("referredEmail", referred == null ? null : referred.getEmail());
                    row.put("level", relationship.getReferralLevel());
                    row.put("active", relationship.isActive());
                    row.put("linkedAt", relationship.getLinkedAt());
                    return row;
                })
                .toList();

        return Map.of(
                "totalReferralUsers", relationships.stream().map(ReferralRelationship::getReferredUserId).collect(Collectors.toSet()).size(),
                "activeLinks", relationships.stream().filter(ReferralRelationship::isActive).count(),
                "totalRelationships", relationships.size(),
                "totalCommissions", totalCommissions,
                "creditedCommissions", creditedCommissions,
                "skippedCommissions", skippedCommissions,
                "levelSummary", levelSummary,
                "topReferrers", topReferrers,
                "recentRelationships", recentRelationships
        );
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
    public User updateUserStatus(User admin, String id, ApiDtos.UpdateUserStatusRequest body, HttpServletRequest request) {
        DomainEnums.AccountStatus nextStatus = resolveUserStatusUpdate(body);
        User user = getUser(id);
        DomainEnums.AccountStatus previousStatus = user.getAccountStatus();
        user.setAccountStatus(nextStatus);
        if (nextStatus == DomainEnums.AccountStatus.ACTIVE) {
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            user.setOnboardingStatus(DomainEnums.OnboardingStatus.ACCOUNT_ACTIVATED);
        }
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        auditService.log(admin, nextStatus == DomainEnums.AccountStatus.ACTIVE ? "USER_ACTIVATED" : "USER_DEACTIVATED", "User", id,
                previousStatus == null ? null : previousStatus.name(), nextStatus.name(), request);
        return saved;
    }

    private DomainEnums.AccountStatus resolveUserStatusUpdate(ApiDtos.UpdateUserStatusRequest body) {
        if (body == null) {
            return DomainEnums.AccountStatus.ACTIVE;
        }
        if (body.isActive() != null) {
            return body.isActive() ? DomainEnums.AccountStatus.ACTIVE : DomainEnums.AccountStatus.DEACTIVATED;
        }
        if (body.active() != null) {
            return body.active() ? DomainEnums.AccountStatus.ACTIVE : DomainEnums.AccountStatus.DEACTIVATED;
        }
        String status = body.accountStatus() == null ? body.status() : body.accountStatus();
        if (status == null || status.isBlank()) {
            return DomainEnums.AccountStatus.ACTIVE;
        }
        DomainEnums.AccountStatus accountStatus;
        try {
            accountStatus = DomainEnums.AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account status");
        }
        if (accountStatus == DomainEnums.AccountStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the suspend endpoint to suspend users");
        }
        return accountStatus;
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

    public Map<String, Object> getNotificationSummary(User user) {
        return Map.of(
                "totalNotifications", notificationRepository.findByUserIdOrderBySentAtDesc(user.getId()).size(),
                "unreadNotifications", notificationRepository.countByUserIdAndReadFlagFalse(user.getId())
        );
    }

    @Transactional
    public Notification markNotificationRead(User user, String id, HttpServletRequest request) {
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.isReadFlag()) {
            notification.setReadFlag(true);
            notification.setReadAt(LocalDateTime.now());
        }
        Notification saved = notificationRepository.save(notification);
        auditService.log(user, "NOTIFICATION_READ", "Notification", id, null, "READ", request);
        return saved;
    }

    @Transactional
    public Map<String, Object> markAllNotificationsRead(User user, HttpServletRequest request) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderBySentAtDesc(user.getId());
        long updatedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : notifications) {
            if (!notification.isReadFlag()) {
                notification.setReadFlag(true);
                notification.setReadAt(now);
                updatedCount++;
            }
        }
        notificationRepository.saveAll(notifications);
        auditService.log(user, "NOTIFICATIONS_READ_ALL", "Notification", user.getId(), null, String.valueOf(updatedCount), request);
        return Map.of(
                "message", updatedCount == 0 ? "All notifications were already read" : "Notifications marked as read",
                "updatedCount", updatedCount
        );
    }

    @Transactional
    public Map<String, Object> deleteNotification(User user, String id, HttpServletRequest request) {
        Notification notification = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notificationRepository.delete(notification);
        auditService.log(user, "NOTIFICATION_DELETED", "Notification", id, null, "DELETED", request);
        return Map.of(
                "message", "Notification deleted successfully",
                "id", id
        );
    }

    @Transactional
    public SupportTicket createSupportTicket(User user, ApiDtos.CreateSupportTicketRequest body, HttpServletRequest request) {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(UUID.randomUUID().toString());
        ticket.setUserId(user.getId());
        ticket.setCategory(firstNonBlank(body.category(), "GENERAL"));
        ticket.setSubject(body.subject());
        ticket.setMessage(body.message());
        ticket.setPriority(resolveSupportPriority(body.priority()));
        ticket.setStatus(DomainEnums.SupportTicketStatus.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        SupportTicket saved = supportTicketRepository.save(ticket);
        auditService.log(user, "SUPPORT_TICKET_CREATED", "SupportTicket", saved.getId(), null, saved.getSubject(), request);
        return saved;
    }

    public List<SupportTicket> getOwnSupportTickets(User user) {
        return supportTicketRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<SupportTicket> getAllSupportTickets() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public SupportTicket respondSupportTicket(User admin, String id, ApiDtos.RespondSupportTicketRequest body, HttpServletRequest request) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Support ticket not found"));
        ticket.setStatus(resolveSupportStatus(body.status(), ticket.getStatus()));
        if (!isBlank(body.adminReply())) {
            ticket.setAdminReply(body.adminReply());
            ticket.setRespondedByAdminId(admin.getId());
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        if (ticket.getStatus() == DomainEnums.SupportTicketStatus.RESOLVED || ticket.getStatus() == DomainEnums.SupportTicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        notifyUser(saved.getUserId(), "Support ticket updated", "Ticket " + saved.getSubject() + " is now " + saved.getStatus(), DomainEnums.NotificationType.SYSTEM);
        auditService.log(admin, "SUPPORT_TICKET_UPDATED", "SupportTicket", id, null, saved.getStatus().name(), request);
        return saved;
    }

    public Map<String, Object> getInvestorStatements(User user) {
        List<WalletTransaction> transactions = walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<InterestRecord> interestRecords = interestRecordRepository.findByInvestorIdOrderByCalculatedAtDesc(user.getId());
        List<ReferralCommission> commissions = referralCommissionRepository.findByBeneficiaryUserIdOrderByCreditedAtDesc(user.getId());
        BigDecimal totalCredits = transactions.stream()
                .filter(tx -> tx.getDirection() == DomainEnums.Direction.CREDIT)
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDebits = transactions.stream()
                .filter(tx -> tx.getDirection() == DomainEnums.Direction.DEBIT)
                .map(WalletTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInterest = interestRecords.stream().map(InterestRecord::getInterestAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReferral = commissions.stream().map(ReferralCommission::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "walletTransactions", transactions,
                "interestRecords", interestRecords,
                "referralCommissions", commissions,
                "summary", Map.of(
                        "totalCredits", totalCredits,
                        "totalDebits", totalDebits,
                        "totalInterest", totalInterest,
                        "totalReferral", totalReferral
                )
        );
    }

    public Map<String, Object> getSecuritySummary(User user) {
        List<AuditLog> events = auditLogRepository.findAllByOrderByOccurredAtDesc().stream()
                .filter(log -> user.getId().equals(log.getActorUserId()))
                .limit(25)
                .toList();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("emailVerified", user.isEmailVerified());
        response.put("bankVerified", user.isBankVerified());
        response.put("biometricEnabled", user.isBiometricEnabled());
        response.put("mpinCreated", !isBlank(user.getMpinHash()));
        response.put("lastLoginAt", user.getLastLoginAt());
        response.put("lastLoginIp", firstNonBlank(user.getLastLoginIp(), "-"));
        response.put("recentEvents", events);
        return response;
    }

    public Map<String, Object> getAdminUser360(String userId) {
        User user = getUser(userId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("profile", user);
        response.put("wallet", walletRepository.findByUserId(userId).orElse(null));
        response.put("kyc", kycSubmissionRepository.findTopByUserIdOrderBySubmittedAtDesc(userId).orElse(null));
        response.put("investments", investmentRepository.findByInvestorUserId(userId));
        response.put("walletTransactions", walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId));
        response.put("withdrawals", withdrawalRepository.findByInvestorIdOrderByRequestedAtDesc(userId));
        response.put("referralTree", getReferralTree(user));
        response.put("referralCommissions", referralCommissionRepository.findByBeneficiaryUserIdOrderByCreditedAtDesc(userId));
        response.put("supportTickets", supportTicketRepository.findByUserIdOrderByCreatedAtDesc(userId));
        response.put("auditEvents", auditLogRepository.findAllByOrderByOccurredAtDesc().stream()
                .filter(log -> userId.equals(log.getActorUserId()) || userId.equals(log.getEntityId()))
                .limit(50)
                .toList());
        return response;
    }

    public Map<String, Object> getFraudRuleSummary() {
        List<User> investors = userRepository.findByRole(DomainEnums.Role.INVESTOR);
        List<Map<String, Object>> duplicatePan = duplicateUserField(investors, User::getPanNumber, "PAN");
        List<Map<String, Object>> duplicateAadhaarLast4 = duplicateUserField(investors, User::getAadhaarLast4, "Aadhaar last 4");
        List<Map<String, Object>> duplicateBank = duplicateUserField(investors, User::getBankAccountNumber, "Bank account");
        List<Map<String, Object>> highVelocityReferrers = referralRelationshipRepository.findAll().stream()
                .collect(Collectors.groupingBy(ReferralRelationship::getReferrerUserId, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() >= 10)
                .map(entry -> {
                    User referrer = userRepository.findById(entry.getKey()).orElse(null);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("rule", "High referral velocity");
                    row.put("userId", entry.getKey());
                    row.put("name", referrer == null ? "Unknown user" : referrer.getFullName());
                    row.put("count", entry.getValue());
                    row.put("severity", entry.getValue() >= 25 ? "HIGH" : "MEDIUM");
                    return row;
                })
                .toList();
        return Map.of(
                "duplicatePan", duplicatePan,
                "duplicateAadhaarLast4", duplicateAadhaarLast4,
                "duplicateBankAccounts", duplicateBank,
                "highVelocityReferrers", highVelocityReferrers,
                "totalSignals", duplicatePan.size() + duplicateAadhaarLast4.size() + duplicateBank.size() + highVelocityReferrers.size()
        );
    }

    public Map<String, Object> getReferralCommissionReview() {
        List<ReferralCommission> commissions = referralCommissionRepository.findAll();
        Map<String, User> usersById = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, user -> user));
        List<Map<String, Object>> rows = commissions.stream()
                .sorted(Comparator.comparing(ReferralCommission::getCreditedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(commission -> {
                    User beneficiary = usersById.get(commission.getBeneficiaryUserId());
                    User source = usersById.get(commission.getSourceInvestorId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", commission.getId());
                    row.put("beneficiaryName", beneficiary == null ? "Unknown user" : beneficiary.getFullName());
                    row.put("beneficiaryStatus", beneficiary == null ? null : beneficiary.getAccountStatus());
                    row.put("sourceInvestorName", source == null ? "Unknown user" : source.getFullName());
                    row.put("sourceInvestmentId", commission.getSourceInvestmentId());
                    row.put("month", commission.getCommissionMonth());
                    row.put("level", commission.getReferralLevel());
                    row.put("rate", commission.getCommissionRate());
                    row.put("sourceInterestAmount", commission.getSourceInterestAmount());
                    row.put("commissionAmount", commission.getCommissionAmount());
                    row.put("status", commission.getStatus());
                    row.put("skipReason", commission.getSkipReason());
                    row.put("creditedAt", commission.getCreditedAt());
                    return row;
                })
                .toList();
        return Map.of(
                "commissions", rows,
                "total", rows.size(),
                "credited", commissions.stream().filter(c -> c.getStatus() == DomainEnums.CommissionStatus.CREDITED).count(),
                "skipped", commissions.stream().filter(c -> c.getStatus() == DomainEnums.CommissionStatus.SKIPPED).count()
        );
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
        Optional<User> directReferrer = userRepository.findByReferralCode(newUser.getReferredByCode().trim().toUpperCase());
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
            commission.setCommissionRate(getReferralRates().get(relationship.getReferralLevel()));
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

    private void processInvestmentActivationReferralCommissions(Investment investment) {
        if (referralCommissionRepository.existsBySourceInvestmentIdAndCommissionMonth(
                investment.getId(), INVESTMENT_ACTIVATION_COMMISSION_MONTH)) {
            return;
        }

        List<ReferralRelationship> uplines = referralRelationshipRepository
                .findByReferredUserIdOrderByReferralLevelAsc(investment.getInvestorUserId());
        for (ReferralRelationship relationship : uplines) {
            BigDecimal rate = getReferralRates().get(relationship.getReferralLevel());
            if (rate == null) {
                continue;
            }

            ReferralCommission commission = new ReferralCommission();
            commission.setId(UUID.randomUUID().toString());
            commission.setBeneficiaryUserId(relationship.getReferrerUserId());
            commission.setSourceInvestorId(investment.getInvestorUserId());
            commission.setSourceInvestmentId(investment.getId());
            commission.setCommissionMonth(INVESTMENT_ACTIVATION_COMMISSION_MONTH);
            commission.setReferralLevel(relationship.getReferralLevel());
            commission.setCommissionRate(rate);
            commission.setSourceInterestAmount(investment.getInvestmentAmount());

            BigDecimal amount = investment.getInvestmentAmount()
                    .multiply(rate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            commission.setCommissionAmount(amount);

            User beneficiary = getUser(relationship.getReferrerUserId());
            if (beneficiary.getAccountStatus() == DomainEnums.AccountStatus.ACTIVE) {
                commission.setStatus(DomainEnums.CommissionStatus.CREDITED);
                commission.setCreditedAt(LocalDateTime.now());
                Wallet wallet = getWalletByUserId(beneficiary.getId());
                creditWallet(
                        wallet,
                        beneficiary.getId(),
                        amount,
                        DomainEnums.WalletTransactionType.REFERRAL_COMMISSION,
                        commission.getId(),
                        "Referral commission credited on investment activation",
                        "SYSTEM"
                );
                notifyUser(
                        beneficiary.getId(),
                        "Referral commission credited",
                        amount + " credited after your referral activated an investment.",
                        DomainEnums.NotificationType.REFERRAL_COMMISSION
                );
            } else {
                commission.setStatus(DomainEnums.CommissionStatus.SKIPPED);
                commission.setSkipReason("Beneficiary inactive");
            }

            referralCommissionRepository.save(commission);
        }
    }

    private Map<String, Object> resolveCouponPreview(User user, BigDecimal investmentAmount, String couponCode, boolean previewOnly) {
        if (isBlank(couponCode)) {
            return couponPreview(null, null, BigDecimal.ZERO, true, "No coupon applied");
        }

        String normalizedCode = couponCode.trim().toUpperCase();
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalizedCode).orElse(null);
        if (coupon == null) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon code not found");
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() != DomainEnums.CouponStatus.ACTIVE) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon is not active");
        }
        if (coupon.getValidFrom() != null && coupon.getValidFrom().isAfter(now)) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon is not valid yet");
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(now)) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon has expired");
        }
        if (coupon.getMinimumInvestmentAmount() != null && investmentAmount.compareTo(coupon.getMinimumInvestmentAmount()) < 0) {
            return invalidCoupon(previewOnly, normalizedCode, "Investment amount is below coupon minimum");
        }
        if (coupon.isFirstInvestmentOnly() && hasPriorInvestment(user.getId())) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon is only for first investment");
        }

        List<DomainEnums.CouponRedemptionStatus> countedStatuses = List.of(
                DomainEnums.CouponRedemptionStatus.RESERVED,
                DomainEnums.CouponRedemptionStatus.CREDITED
        );
        if (coupon.getTotalUsageLimit() != null && coupon.getTotalUsageLimit() > 0
                && couponRedemptionRepository.countByCouponIdAndStatusIn(coupon.getId(), countedStatuses) >= coupon.getTotalUsageLimit()) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon usage limit reached");
        }
        if (coupon.getPerUserUsageLimit() != null && coupon.getPerUserUsageLimit() > 0
                && couponRedemptionRepository.countByCouponIdAndUserIdAndStatusIn(coupon.getId(), user.getId(), countedStatuses) >= coupon.getPerUserUsageLimit()) {
            return invalidCoupon(previewOnly, normalizedCode, "You have already used this coupon");
        }

        BigDecimal cashback = calculateCouponCashback(coupon, investmentAmount);
        if (cashback.compareTo(BigDecimal.ZERO) <= 0) {
            return invalidCoupon(previewOnly, normalizedCode, "Coupon has no cashback for this amount");
        }

        return couponPreview(coupon.getId(), coupon.getCode(), cashback, true, "Coupon applied");
    }

    private Map<String, Object> invalidCoupon(boolean previewOnly, String couponCode, String message) {
        if (!previewOnly) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return couponPreview(null, couponCode, BigDecimal.ZERO, false, message);
    }

    private Map<String, Object> couponPreview(String couponId, String couponCode, BigDecimal cashbackAmount, boolean valid, String message) {
        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("couponId", couponId);
        preview.put("couponCode", couponCode);
        preview.put("cashbackAmount", cashbackAmount == null ? BigDecimal.ZERO : cashbackAmount);
        preview.put("valid", valid);
        preview.put("message", message);
        return preview;
    }

    private BigDecimal calculateCouponCashback(Coupon coupon, BigDecimal investmentAmount) {
        BigDecimal cashback;
        if (coupon.getType() == DomainEnums.CouponType.PERCENT_CASHBACK) {
            cashback = investmentAmount.multiply(coupon.getValueAmount()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        } else {
            cashback = coupon.getValueAmount();
        }
        if (coupon.getMaximumCashbackAmount() != null && coupon.getMaximumCashbackAmount().compareTo(BigDecimal.ZERO) > 0
                && cashback.compareTo(coupon.getMaximumCashbackAmount()) > 0) {
            cashback = coupon.getMaximumCashbackAmount();
        }
        return cashback.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean hasPriorInvestment(String userId) {
        return investmentRepository.findByInvestorUserId(userId).stream()
                .anyMatch(investment -> investment.getStatus() != DomainEnums.InvestmentStatus.CANCELLED
                        && investment.getStatus() != DomainEnums.InvestmentStatus.REJECTED);
    }

    private void reserveCouponRedemption(User user, Investment investment, Map<String, Object> couponPreview) {
        String couponId = (String) couponPreview.get("couponId");
        if (isBlank(couponId)) {
            return;
        }

        CouponRedemption redemption = new CouponRedemption();
        redemption.setId(UUID.randomUUID().toString());
        redemption.setCouponId(couponId);
        redemption.setCouponCode((String) couponPreview.get("couponCode"));
        redemption.setUserId(user.getId());
        redemption.setInvestmentId(investment.getId());
        redemption.setInvestmentAmount(investment.getInvestmentAmount());
        redemption.setCashbackAmount((BigDecimal) couponPreview.get("cashbackAmount"));
        redemption.setStatus(DomainEnums.CouponRedemptionStatus.RESERVED);
        redemption.setRedeemedAt(LocalDateTime.now());
        couponRedemptionRepository.save(redemption);
    }

    private void creditCouponCashback(Investment investment) {
        if (investment.isCouponCredited() || investment.getCouponCashbackAmount() == null
                || investment.getCouponCashbackAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        CouponRedemption redemption = couponRedemptionRepository.findByInvestmentId(investment.getId()).orElse(null);
        if (redemption == null || redemption.getStatus() == DomainEnums.CouponRedemptionStatus.CREDITED) {
            return;
        }

        Wallet wallet = getWalletByUserId(investment.getInvestorUserId());
        creditWallet(wallet, investment.getInvestorUserId(), investment.getCouponCashbackAmount(),
                DomainEnums.WalletTransactionType.COUPON_CASHBACK, redemption.getId(),
                "Coupon cashback credited: " + investment.getAppliedCouponCode(), "SYSTEM");

        redemption.setStatus(DomainEnums.CouponRedemptionStatus.CREDITED);
        redemption.setCreditedAt(LocalDateTime.now());
        couponRedemptionRepository.save(redemption);

        investment.setCouponCredited(true);
        investmentRepository.save(investment);
        notifyUser(investment.getInvestorUserId(), "Coupon cashback credited",
                investment.getCouponCashbackAmount() + " credited for coupon " + investment.getAppliedCouponCode(),
                DomainEnums.NotificationType.INVESTMENT_UPDATE);
    }

    private void cancelCouponRedemption(String investmentId) {
        couponRedemptionRepository.findByInvestmentId(investmentId)
                .filter(redemption -> redemption.getStatus() == DomainEnums.CouponRedemptionStatus.RESERVED)
                .ifPresent(redemption -> {
                    redemption.setStatus(DomainEnums.CouponRedemptionStatus.CANCELLED);
                    couponRedemptionRepository.save(redemption);
                });
    }

    private Map<Integer, BigDecimal> getReferralRates() {
        Map<Integer, BigDecimal> rates = new LinkedHashMap<>(REFERRAL_RATES);
        for (int level = 1; level <= 5; level++) {
            int referralLevel = level;
            platformSettingRepository.findById("referral.rate.level" + referralLevel)
                    .map(PlatformSetting::getSettingValue)
                    .map(this::parsePositiveDecimal)
                    .ifPresent(rate -> rates.put(referralLevel, rate));
        }
        return rates;
    }

    private BigDecimal parsePositiveDecimal(String value) {
        try {
            BigDecimal parsed = new BigDecimal(value);
            return parsed.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : parsed;
        } catch (RuntimeException ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal sanitizeReferralRate(BigDecimal rate, BigDecimal fallback) {
        BigDecimal resolved = rate == null ? fallback : rate;
        if (resolved == null || resolved.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (resolved.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100");
        }
        return resolved;
    }

    private void upsertSetting(String key, String value, String updatedBy) {
        PlatformSetting setting = platformSettingRepository.findById(key).orElseGet(PlatformSetting::new);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(updatedBy);
        platformSettingRepository.save(setting);
    }

    private void applyCouponFields(Coupon coupon, String title, String description, String type, BigDecimal valueAmount,
                                   BigDecimal minimumInvestmentAmount, BigDecimal maximumCashbackAmount,
                                   Integer totalUsageLimit, Integer perUserUsageLimit, boolean firstInvestmentOnly,
                                   String validFrom, String validUntil, String status, String adminId, boolean create) {
        coupon.setTitle(title);
        coupon.setDescription(description);
        coupon.setType(resolveCouponType(type));
        coupon.setValueAmount(valueAmount);
        coupon.setMinimumInvestmentAmount(minimumInvestmentAmount == null ? BigDecimal.ZERO : minimumInvestmentAmount);
        coupon.setMaximumCashbackAmount(maximumCashbackAmount);
        coupon.setTotalUsageLimit(totalUsageLimit);
        coupon.setPerUserUsageLimit(perUserUsageLimit == null || perUserUsageLimit <= 0 ? 1 : perUserUsageLimit);
        coupon.setFirstInvestmentOnly(firstInvestmentOnly);
        coupon.setStatus(resolveCouponStatus(status));
        coupon.setValidFrom(parseDateTime(validFrom));
        coupon.setValidUntil(parseDateTime(validUntil));
        if (create) {
            coupon.setCreatedByAdminId(adminId);
            coupon.setCreatedAt(LocalDateTime.now());
        }
        coupon.setLastModifiedAt(LocalDateTime.now());
        coupon.setLastModifiedBy(adminId);
    }

    private DomainEnums.CouponType resolveCouponType(String value) {
        if (isBlank(value)) {
            return DomainEnums.CouponType.FLAT_CASHBACK;
        }
        return DomainEnums.CouponType.valueOf(value.trim().toUpperCase());
    }

    private DomainEnums.CouponStatus resolveCouponStatus(String value) {
        if (isBlank(value)) {
            return DomainEnums.CouponStatus.ACTIVE;
        }
        return DomainEnums.CouponStatus.valueOf(value.trim().toUpperCase());
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (RuntimeException ignored) {
            return LocalDate.parse(value.trim()).atStartOfDay();
        }
    }

    private DomainEnums.SupportTicketPriority resolveSupportPriority(String value) {
        if (isBlank(value)) {
            return DomainEnums.SupportTicketPriority.MEDIUM;
        }
        try {
            return DomainEnums.SupportTicketPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DomainEnums.SupportTicketPriority.MEDIUM;
        }
    }

    private DomainEnums.SupportTicketStatus resolveSupportStatus(String value, DomainEnums.SupportTicketStatus fallback) {
        if (isBlank(value)) {
            return fallback == null ? DomainEnums.SupportTicketStatus.OPEN : fallback;
        }
        try {
            return DomainEnums.SupportTicketStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback == null ? DomainEnums.SupportTicketStatus.OPEN : fallback;
        }
    }

    private List<Map<String, Object>> duplicateUserField(List<User> users, Function<User, String> fieldReader, String rule) {
        return users.stream()
                .filter(user -> !isBlank(fieldReader.apply(user)))
                .collect(Collectors.groupingBy(user -> fieldReader.apply(user).trim().toUpperCase()))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("rule", "Duplicate " + rule);
                    row.put("value", maskSensitive(entry.getKey()));
                    row.put("count", entry.getValue().size());
                    row.put("severity", entry.getValue().size() >= 3 ? "HIGH" : "MEDIUM");
                    row.put("users", entry.getValue().stream()
                            .map(user -> {
                                Map<String, Object> userRow = new LinkedHashMap<>();
                                userRow.put("userId", user.getId());
                                userRow.put("name", user.getFullName());
                                userRow.put("email", user.getEmail());
                                userRow.put("accountStatus", user.getAccountStatus());
                                return userRow;
                            })
                            .toList());
                    return row;
                })
                .toList();
    }

    private String maskSensitive(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    @Transactional
    public Map<String, Object> syncRazorpayPayment(User admin, String investmentId, HttpServletRequest request) {
        Investment investment = getInvestment(investmentId);
        RazorpayPayment razorpayPayment = razorpayPaymentRepository.findByInvestmentId(investmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Razorpay payment not found"));
        if (razorpayPayment.getRazorpayPaymentId() == null || razorpayPayment.getRazorpayPaymentId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Razorpay payment id is available yet");
        }
        Map<String, Object> paymentPayload = refreshPaymentFromGateway(razorpayPayment);
        if (Boolean.TRUE.equals(razorpayPayment.getCaptured()) || "captured".equalsIgnoreCase(razorpayPayment.getStatus())) {
            finalizeCapturedInvestmentPayment(investment, razorpayPayment, paymentPayload, "ADMIN_SYNC");
        } else {
            razorpayPaymentRepository.save(razorpayPayment);
        }
        auditService.log(admin, "RAZORPAY_PAYMENT_SYNCED", "RazorpayPayment", razorpayPayment.getId(), null, razorpayPayment.getRazorpayPaymentId(), request);
        return Map.of(
                "investment", investment,
                "payment", razorpayPayment
        );
    }

    @Transactional
    public Map<String, Object> handleRazorpayWebhook(String signature, String eventId, String payload) {
        if (signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Razorpay webhook signature");
        }
        if (!razorpayGatewayService.verifyWebhookSignature(payload, signature)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay webhook signature");
        }
        if (eventId != null && razorpayPaymentRepository.findByWebhookEventId(eventId).isPresent()) {
            return Map.of("status", "duplicate", "eventId", eventId);
        }

        Map<String, Object> webhook = readJsonMap(payload);
        String eventType = asText(webhook.get("event"));
        Map<String, Object> payloadNode = mapValue(webhook.get("payload"));
        Map<String, Object> paymentWrapper = mapValue(payloadNode.get("payment"));
        Map<String, Object> paymentEntity = mapValue(paymentWrapper.get("entity"));

        if (paymentEntity.isEmpty()) {
            return Map.of("status", "ignored", "event", eventType);
        }

        String orderId = asText(paymentEntity.get("order_id"));
        String paymentId = asText(paymentEntity.get("id"));
        RazorpayPayment razorpayPayment = findRazorpayPayment(orderId, paymentId);
        if (razorpayPayment == null) {
            return Map.of("status", "unmapped", "event", eventType, "orderId", orderId, "paymentId", paymentId);
        }

        razorpayPayment.setWebhookEventId(eventId);
        razorpayPayment.setWebhookEventType(eventType);
        razorpayPayment.setWebhookPayload(payload);
        razorpayPayment.setRazorpayPaymentId(paymentId);
        applyPaymentSnapshot(razorpayPayment, paymentEntity);

        Investment investment = getInvestment(razorpayPayment.getInvestmentId());
        if (Boolean.TRUE.equals(razorpayPayment.getCaptured()) || "captured".equalsIgnoreCase(razorpayPayment.getStatus())
                || "order.paid".equalsIgnoreCase(eventType)) {
            finalizeCapturedInvestmentPayment(investment, razorpayPayment, paymentEntity, "WEBHOOK");
        } else {
            razorpayPaymentRepository.save(razorpayPayment);
        }

        return Map.of("status", "processed", "eventId", eventId, "event", eventType);
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

    private Coupon getCoupon(String id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found"));
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

    private Investment activateInvestmentRecord(Investment investment, String activatedBy, String notes) {
        InvestmentPlan plan = getPlan(investment.getInvestmentPlanId());
        investment.setReceiptApproved(true);
        investment.setStatus(DomainEnums.InvestmentStatus.ACTIVE);
        investment.setActivatedAt(LocalDateTime.now());
        investment.setActivatedByAdminId(activatedBy);
        investment.setMaturityDate(LocalDate.now().plusMonths(plan.getLockInMonths()));
        investment.setMonthlyInterestRate(plan.getMonthlyInterestRate());
        investment.setNotes(notes);
        Investment saved = investmentRepository.save(investment);
        processInvestmentActivationReferralCommissions(saved);
        creditCouponCashback(saved);
        return saved;
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

    private Map<String, Object> refreshPaymentFromGateway(RazorpayPayment razorpayPayment) {
        Map<String, Object> paymentPayload = razorpayGatewayService.fetchPayment(razorpayPayment.getRazorpayPaymentId());
        applyPaymentSnapshot(razorpayPayment, paymentPayload);
        return paymentPayload;
    }

    private void applyPaymentSnapshot(RazorpayPayment razorpayPayment, Map<String, Object> paymentPayload) {
        razorpayPayment.setRazorpayOrderId(firstNonBlank(razorpayPayment.getRazorpayOrderId(), asText(paymentPayload.get("order_id"))));
        razorpayPayment.setRazorpayPaymentId(firstNonBlank(razorpayPayment.getRazorpayPaymentId(), asText(paymentPayload.get("id"))));
        razorpayPayment.setStatus(asText(paymentPayload.get("status")));
        razorpayPayment.setMethod(asText(paymentPayload.get("method")));
        razorpayPayment.setCaptured(Boolean.TRUE.equals(paymentPayload.get("captured")));
        razorpayPayment.setCurrency(firstNonBlank(asText(paymentPayload.get("currency")), razorpayPayment.getCurrency()));
        BigDecimal gatewayAmount = subunitsToAmount(paymentPayload.get("amount"));
        if (gatewayAmount != null) {
            razorpayPayment.setAmount(gatewayAmount);
        }
        razorpayPayment.setLastSyncedAt(LocalDateTime.now());
        razorpayPayment.setPaymentPayload(writeJson(paymentPayload));
        if ("authorized".equalsIgnoreCase(razorpayPayment.getStatus()) && razorpayPayment.getPaymentAuthorizedAt() == null) {
            razorpayPayment.setPaymentAuthorizedAt(LocalDateTime.now());
        }
        if (Boolean.TRUE.equals(razorpayPayment.getCaptured()) || "captured".equalsIgnoreCase(razorpayPayment.getStatus())) {
            if (razorpayPayment.getPaymentCapturedAt() == null) {
                razorpayPayment.setPaymentCapturedAt(LocalDateTime.now());
            }
            Map<String, Object> acquirerData = mapValue(paymentPayload.get("acquirer_data"));
            razorpayPayment.setSettlementUtr(firstNonBlank(razorpayPayment.getSettlementUtr(), asText(acquirerData.get("rrn"))));
            razorpayPayment.setSettlementId(firstNonBlank(razorpayPayment.getSettlementId(), asText(paymentPayload.get("settlement_id"))));
            if (razorpayPayment.getSettlementId() != null && !razorpayPayment.getSettlementId().isBlank()) {
                razorpayPayment.setSettlementStatus("PENDING_FROM_RAZORPAY");
            }
        }
    }

    private void finalizeCapturedInvestmentPayment(Investment investment, RazorpayPayment razorpayPayment,
                                                   Map<String, Object> paymentPayload, String source) {
        applyPaymentSnapshot(razorpayPayment, paymentPayload);
        if (investment.getStatus() != DomainEnums.InvestmentStatus.ACTIVE) {
            activateInvestmentRecord(investment, source, "Activated via Razorpay " + source);
            notifyUser(investment.getInvestorUserId(), "Investment activated", "Payment captured successfully. Investment is active now.", DomainEnums.NotificationType.INVESTMENT_UPDATE);
        }
        razorpayPaymentRepository.save(razorpayPayment);
        upsertRazorpayReceipt(investment, razorpayPayment);
    }

    private void upsertRazorpayReceipt(Investment investment, RazorpayPayment razorpayPayment) {
        PaymentReceipt receipt = paymentReceiptRepository.findTopByInvestmentIdOrderByUploadedAtDesc(investment.getId()).orElseGet(PaymentReceipt::new);
        receipt.setId(receipt.getId() == null ? UUID.randomUUID().toString() : receipt.getId());
        receipt.setInvestmentId(investment.getId());
        receipt.setInvestorId(investment.getInvestorUserId());
        receipt.setFileName("razorpay:" + razorpayPayment.getRazorpayOrderId());
        receipt.setFileType("application/json");
        receipt.setFileSize(0L);
        receipt.setPaymentAmount(razorpayPayment.getAmount());
        receipt.setPaymentDate(LocalDate.now());
        receipt.setPaymentMode(toPaymentMode(razorpayPayment.getMethod()));
        receipt.setBankReference(razorpayPayment.getRazorpayPaymentId());
        receipt.setVerificationStatus(DomainEnums.ReceiptStatus.APPROVED);
        receipt.setVerifiedByAdminId("SYSTEM_RAZORPAY");
        receipt.setVerifiedAt(LocalDateTime.now());
        receipt.setUploadedAt(receipt.getUploadedAt() == null ? LocalDateTime.now() : receipt.getUploadedAt());
        paymentReceiptRepository.save(receipt);
    }

    private DomainEnums.PaymentMode toPaymentMode(String method) {
        if (method == null) {
            return DomainEnums.PaymentMode.UPI;
        }
        return switch (method.toLowerCase()) {
            case "upi" -> DomainEnums.PaymentMode.UPI;
            case "netbanking" -> DomainEnums.PaymentMode.NETBANKING;
            case "card" -> DomainEnums.PaymentMode.CARD;
            case "wallet" -> DomainEnums.PaymentMode.WALLET;
            default -> DomainEnums.PaymentMode.UPI;
        };
    }

    private RazorpayPayment findRazorpayPayment(String orderId, String paymentId) {
        if (paymentId != null && !paymentId.isBlank()) {
            Optional<RazorpayPayment> byPaymentId = razorpayPaymentRepository.findByRazorpayPaymentId(paymentId);
            if (byPaymentId.isPresent()) {
                return byPaymentId.get();
            }
        }
        if (orderId != null && !orderId.isBlank()) {
            return razorpayPaymentRepository.findByRazorpayOrderId(orderId).orElse(null);
        }
        return null;
    }

    private Map<String, Object> readJsonMap(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Razorpay webhook payload");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String writeJson(Map<String, Object> payload) {
        return razorpayGatewayService.writeJson(payload);
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal subunitsToAmount(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value)).movePointLeft(2);
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
