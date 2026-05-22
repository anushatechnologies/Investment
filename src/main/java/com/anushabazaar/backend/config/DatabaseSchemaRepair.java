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
        repairColumn("notification", "type");
        repairColumn("notification", "channel");
        repairColumn("audit_log", "actor_role");
    }

    private void repairColumn(String table, String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " varchar(50)");
            log.info("Ensured {}.{} uses varchar storage", table, column);
        } catch (Exception ex) {
            log.warn("Could not repair {}.{} automatically: {}", table, column, ex.getMessage());
        }
    }
}
