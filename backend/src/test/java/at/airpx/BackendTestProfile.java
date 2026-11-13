package at.airpx;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class BackendTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "auth.bootstrap.username", "Testuser",
                "auth.bootstrap.email", "test@example.com",
                "auth.bootstrap.password", "correct horse battery staple",
                "photo.storage.directory", "target/test-photos"
        );
    }
}
