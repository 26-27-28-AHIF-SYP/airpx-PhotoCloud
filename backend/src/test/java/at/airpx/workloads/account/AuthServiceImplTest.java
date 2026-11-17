package at.airpx.workloads.account;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(at.airpx.BackendTestProfile.class)
class AuthServiceImplTest {

    @Inject AuthServiceImpl authService;

    @Test
    void bootstrapSkipsExistingConfiguredUser() {
        authService.bootstrap(null);

        assertTrue(authService.login("test@example.com", "correct horse battery staple").isPresent());
    }
}
