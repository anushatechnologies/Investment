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
        try {
            jdbcTemplate.execute("ALTER TABLE token_record MODIFY COLUMN token_type varchar(50)");
            log.info("Ensured token_record.token_type uses varchar storage");
        } catch (Exception ex) {
            log.warn("Could not repair token_record.token_type column automatically: {}", ex.getMessage());
        }
    }
}
