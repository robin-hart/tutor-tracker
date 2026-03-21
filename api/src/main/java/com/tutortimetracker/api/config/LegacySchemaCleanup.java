package com.tutortimetracker.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Performs lightweight schema cleanup for legacy local databases.
 */
@Component
@Profile("!test")
public class LegacySchemaCleanup implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacySchemaCleanup.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * @param jdbcTemplate JDBC helper for raw schema operations
     */
    public LegacySchemaCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'students'
                  AND column_name = 'last_active'
                """,
                Integer.class
        );

        if (columnCount != null && columnCount > 0) {
            jdbcTemplate.execute("ALTER TABLE students DROP COLUMN last_active");
            LOGGER.info("Dropped legacy column students.last_active");
        }
    }
}
