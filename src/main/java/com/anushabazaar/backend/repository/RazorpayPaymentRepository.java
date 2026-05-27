package com.anushabazaar.backend.repository;

import com.anushabazaar.backend.domain.RazorpayPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RazorpayPaymentRepository extends JpaRepository<RazorpayPayment, String> {
    Optional<RazorpayPayment> findByInvestmentId(String investmentId);
    Optional<RazorpayPayment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<RazorpayPayment> findByRazorpayPaymentId(String razorpayPaymentId);
    Optional<RazorpayPayment> findByWebhookEventId(String webhookEventId);
    List<RazorpayPayment> findByInvestorIdOrderByCheckoutOrderCreatedAtDesc(String investorId);
    List<RazorpayPayment> findAllByOrderByCheckoutOrderCreatedAtDesc();
}
