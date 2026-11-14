package at.airpx;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class FlywayMigrationTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.flyway.migrate-at-start", "true",
                "quarkus.flyway.baseline-on-migrate", "false",
                "quarkus.hibernate-orm.schema-management.strategy", "validate"
        );
    }
}
