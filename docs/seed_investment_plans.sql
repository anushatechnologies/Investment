-- ==============================================================================
-- SEED INVESTMENT PLANS FOR AWS RDS / LOCAL MYSQL DATABASE
-- ==============================================================================
-- 1. Anusha Milk Trade Investment Plan: Min ₹5,000 to ₹10,00,000 | 10% Monthly Interest
-- 2. ₹1 Razorpay Test Payment Plan: Min ₹1 to ₹100 | 10% Monthly Interest (For Live Payment Testing)
-- 3. Anusha Prime Investor Plan: Min ₹1,00,000 to ₹50,00,000 | 12% Monthly Interest
-- ==============================================================================

-- 1. Anusha Milk Trade Investment Plan (Min ₹5,000 to ₹10,00,000 @ 10% Monthly Interest)
INSERT INTO investment_plan (
    id, active, created_at, created_by_admin_id, description, 
    last_modified_at, last_modified_by, lock_in_months, 
    maximum_amount, minimum_amount, monthly_interest_rate, plan_name
) VALUES (
    'PLAN-MILK-TRADE-5K-10L', 1, NOW(), 'SYSTEM', 
    'Official Anusha Milk Trade high-yield investment plan with 10% monthly payout credited directly to your wallet.',
    NOW(), 'SYSTEM', 6, 
    1000000.00, 5000.00, 10.00, 'Anusha Milk Trade Investment Plan'
) ON DUPLICATE KEY UPDATE 
    plan_name = VALUES(plan_name),
    minimum_amount = VALUES(minimum_amount),
    maximum_amount = VALUES(maximum_amount),
    monthly_interest_rate = VALUES(monthly_interest_rate),
    lock_in_months = VALUES(lock_in_months),
    description = VALUES(description),
    active = 1;

-- 2. ₹1 Razorpay Test Payment Plan (Min ₹1 to ₹100 @ 10% Monthly Interest)
INSERT INTO investment_plan (
    id, active, created_at, created_by_admin_id, description, 
    last_modified_at, last_modified_by, lock_in_months, 
    maximum_amount, minimum_amount, monthly_interest_rate, plan_name
) VALUES (
    'PLAN-RAZORPAY-TEST-1INR', 1, NOW(), 'SYSTEM', 
    'Instant ₹1 test investment plan to verify real-time Razorpay payments, UPI, and instant digital receipts.',
    NOW(), 'SYSTEM', 1, 
    100.00, 1.00, 10.00, '₹1 Razorpay Test Payment Plan'
) ON DUPLICATE KEY UPDATE 
    plan_name = VALUES(plan_name),
    minimum_amount = VALUES(minimum_amount),
    maximum_amount = VALUES(maximum_amount),
    monthly_interest_rate = VALUES(monthly_interest_rate),
    lock_in_months = VALUES(lock_in_months),
    description = VALUES(description),
    active = 1;

-- 3. Anusha Prime Investor Plan (Min ₹1,00,000 to ₹50,00,000 @ 12% Monthly Interest)
INSERT INTO investment_plan (
    id, active, created_at, created_by_admin_id, description, 
    last_modified_at, last_modified_by, lock_in_months, 
    maximum_amount, minimum_amount, monthly_interest_rate, plan_name
) VALUES (
    'PLAN-PRIME-INVESTOR-1L-50L', 1, NOW(), 'SYSTEM', 
    'High-Yield 12-Month Lock-in Investment Plan with 12% Monthly Payout for high net-worth investors.',
    NOW(), 'SYSTEM', 12, 
    5000000.00, 100000.00, 12.00, 'Anusha Prime Investor Plan'
) ON DUPLICATE KEY UPDATE 
    plan_name = VALUES(plan_name),
    minimum_amount = VALUES(minimum_amount),
    maximum_amount = VALUES(maximum_amount),
    monthly_interest_rate = VALUES(monthly_interest_rate),
    lock_in_months = VALUES(lock_in_months),
    description = VALUES(description),
    active = 1;

-- Verification Query:
SELECT id, plan_name, minimum_amount, maximum_amount, monthly_interest_rate, lock_in_months, active FROM investment_plan;
