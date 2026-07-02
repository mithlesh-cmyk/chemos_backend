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
}
