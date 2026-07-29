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
public class SalesSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumnExists("lifted_qty");
        ensureColumnExists("remaining_qty");
    }

    private void ensureColumnExists(String columnName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT 1
                FROM information_schema.columns
                WHERE table_name = 'sales_form'
                  AND column_name = ?
                """,
                columnName
        );

        if (!rows.isEmpty()) {
            log.info("Column {} already exists on sales_form.", columnName);
            return;
        }

        log.info("Adding missing {} column to sales_form...", columnName);
        jdbcTemplate.execute(
                "ALTER TABLE sales_form ADD COLUMN " + columnName + " DOUBLE PRECISION"
        );
        log.info("Added {} column to sales_form.", columnName);
    }
}
