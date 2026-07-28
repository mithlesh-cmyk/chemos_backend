package chemos.chem_os.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalePurchaseLinkSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumnExists();
        ensureNegativeColumnExists();
        ensureNegativeHistoryTableExists();
    }

    private void ensureColumnExists() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT 1
                FROM information_schema.columns
                WHERE table_name = 'sale_purchase_links'
                  AND column_name = 'created_by_username'
                """
        );

        if (!rows.isEmpty()) {
            log.info("Column created_by_username already exists on sale_purchase_links.");
            return;
        }

        log.info("Adding missing created_by_username column to sale_purchase_links...");
        jdbcTemplate.execute(
                "ALTER TABLE sale_purchase_links ADD COLUMN created_by_username VARCHAR(255) NOT NULL DEFAULT 'system'"
        );
        log.info("Added created_by_username column to sale_purchase_links.");
    }

    private void ensureNegativeColumnExists() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT 1
                FROM information_schema.columns
                WHERE table_name = 'sale_purchase_links'
                  AND column_name = 'is_negative'
                """
        );

        if (!rows.isEmpty()) {
            log.info("Column is_negative already exists on sale_purchase_links.");
            return;
        }

        log.info("Adding missing is_negative column to sale_purchase_links...");
        jdbcTemplate.execute(
                "ALTER TABLE sale_purchase_links ADD COLUMN is_negative BOOLEAN NOT NULL DEFAULT FALSE"
        );
        log.info("Added is_negative column to sale_purchase_links.");
    }

    private void ensureNegativeHistoryTableExists() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT 1
                FROM information_schema.tables
                WHERE table_name = 'sale_purchase_link_negative_history'
                """
        );

        if (!rows.isEmpty()) {
            log.info("Table sale_purchase_link_negative_history already exists.");
            return;
        }

        log.info("Creating sale_purchase_link_negative_history table...");
        jdbcTemplate.execute(
                """
                CREATE TABLE sale_purchase_link_negative_history (
                    id VARCHAR(255) PRIMARY KEY,
                    link_id VARCHAR(255) NOT NULL,
                    sale_id VARCHAR(255) NOT NULL,
                    purchase_id VARCHAR(255) NOT NULL,
                    linked_quantity DOUBLE PRECISION NOT NULL,
                    purchase_original_quantity DOUBLE PRECISION NOT NULL,
                    purchase_available_quantity DOUBLE PRECISION NOT NULL,
                    action VARCHAR(20) NOT NULL,
                    changed_by_username VARCHAR(255) NOT NULL,
                    occurred_at TIMESTAMP NOT NULL DEFAULT now()
                )
                """
        );
        log.info("Created sale_purchase_link_negative_history table.");
    }
}
