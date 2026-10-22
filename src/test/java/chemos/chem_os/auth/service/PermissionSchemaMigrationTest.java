package chemos.chem_os.auth.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionSchemaMigrationTest {
    @Test
    void manualSchemaIsRepeatableAndDefaultsExistingRolesToNone() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:permission_schema;MODE=PostgreSQL");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE roles (id varchar(255) PRIMARY KEY)");
            statement.execute("CREATE TABLE permissions (id varchar(255) PRIMARY KEY)");
            statement.execute("INSERT INTO roles(id) VALUES ('pur_exe')");
            String script = Files.readString(Path.of("sql", "permission_inheritance_schema.sql"));
            statement.execute(script);
            statement.execute(script);
            try (var result = statement.executeQuery("SELECT inheritance_mode FROM roles WHERE id = 'pur_exe'")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("NONE");
            }
        }
    }
}