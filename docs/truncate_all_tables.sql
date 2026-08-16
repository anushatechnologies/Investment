-- ==============================================================================
-- TRUNCATE ALL TABLE DATA (PRESERVES ALL TABLE SCHEMAS, COLUMNS, & CONSTRAINTS)
-- ==============================================================================
-- This script safely deletes all rows from all application tables without dropping tables.
-- Run this on your MySQL database (Local or AWS RDS).

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE audit_log;
TRUNCATE TABLE bank_account;
TRUNCATE TABLE coupon;
TRUNCATE TABLE coupon_redemption;
TRUNCATE TABLE fraud_alert;
TRUNCATE TABLE interest_record;
TRUNCATE TABLE investment;
TRUNCATE TABLE investment_plan;
TRUNCATE TABLE investment_receipts;
TRUNCATE TABLE kyc_submission;
TRUNCATE TABLE notification;
TRUNCATE TABLE payment_receipt;
TRUNCATE TABLE platform_setting;
TRUNCATE TABLE razorpay_payment;
TRUNCATE TABLE referral_commission;
TRUNCATE TABLE referral_relationship;
TRUNCATE TABLE support_ticket;
TRUNCATE TABLE token_record;
TRUNCATE TABLE wallet_transaction;
TRUNCATE TABLE withdrawal_request;
TRUNCATE TABLE wallet;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- Verification query
SELECT 'users' as tbl, count(*) as cnt FROM users
UNION ALL SELECT 'wallet', count(*) FROM wallet
UNION ALL SELECT 'kyc_submission', count(*) FROM kyc_submission
UNION ALL SELECT 'investment', count(*) FROM investment
UNION ALL SELECT 'payment_receipt', count(*) FROM payment_receipt
UNION ALL SELECT 'token_record', count(*) FROM token_record;
