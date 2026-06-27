package com.anushabazaar.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaRepair {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaRepair.class);

    private final JdbcTemplate jdbcTemplate;
    private final String datasourceUrl;

    public DatabaseSchemaRepair(JdbcTemplate jdbcTemplate,
                                @Value("${spring.datasource.url}") String datasourceUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.datasourceUrl = datasourceUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void repairMysqlEnumColumns() {
        if (datasourceUrl == null || !datasourceUrl.toLowerCase().contains("mysql")) {
            return;
        }
        repairColumn("token_record", "token_type");
        repairColumn("users", "onboarding_status");
        repairColumn("users", "kyc_status");
        repairColumn("users", "account_status");
        repairColumn("users", "role");
        repairColumn("kyc_submission", "status");
        repairColumn("kyc_submission", "pan_card_status");
        repairColumn("kyc_submission", "aadhaar_front_status");
        repairColumn("kyc_submission", "aadhaar_back_status");
        repairColumn("kyc_submission", "selfie_status");
        repairColumn("kyc_submission", "bank_proof_status");
        repairTextColumn("kyc_submission", "rejection_reason");
        repairTextColumn("kyc_submission", "admin_notes");
        repairTextColumn("kyc_submission", "pan_card_rejection_reason");
        repairTextColumn("kyc_submission", "aadhaar_front_rejection_reason");
        repairTextColumn("kyc_submission", "aadhaar_back_rejection_reason");
        repairTextColumn("kyc_submission", "selfie_rejection_reason");
        repairTextColumn("kyc_submission", "bank_proof_rejection_reason");
        repairColumn("investment", "status");
        repairColumn("payment_receipt", "payment_mode");
        repairColumn("payment_receipt", "verification_status");
        repairColumn("wallet_transaction", "transaction_type");
        repairColumn("wallet_transaction", "direction");
        repairColumn("withdrawal_request", "status");
        repairColumn("referral_commission", "status");
        repairColumn("referral_commission", "commission_type");
        repairColumn("interest_record", "status");
        repairColumn("fraud_alert", "alert_level");
        repairColumn("fraud_alert", "status");
        dropCheckConstraint("notification", "notification_chk_1");
        dropCheckConstraint("notification", "notification_chk_2");
        dropCheckConstraint("notification", "notification_chk_3");
        dropCheckConstraint("notification", "notification_chk_4");
        repairColumn("notification", "type");
        repairColumn("notification", "channel");
        repairTextColumn("notification", "message");
        repairColumn("audit_log", "actor_role");
        repairTextColumn("audit_log", "old_value");
        repairTextColumn("audit_log", "new_value");
        repairTextColumn("audit_log", "user_agent");
        repairColumn("coupon", "type");
        repairColumn("coupon", "status");
        repairColumn("coupon_redemption", "status");
    }

    private void repairColumn(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " varchar(50)");
            log.info("Ensured {}.{} uses varchar storage", table, column);
        } catch (Exception ex) {
            log.warn("Could not repair {}.{} automatically: {}", table, column, ex.getMessage());
        }
    }

    private void dropCheckConstraint(String table, String constraint) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP CHECK " + constraint);
            log.info("Dropped stale check constraint {}.{}", table, constraint);
        } catch (Exception ex) {
            log.debug("Check constraint {}.{} was not dropped: {}", table, constraint, ex.getMessage());
        }
    }

    private void repairTextColumn(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " text");
            log.info("Ensured {}.{} uses text storage", table, column);
        } catch (Exception ex) {
            log.warn("Could not repair {}.{} automatically: {}", table, column, ex.getMessage());
        }
    }
}
