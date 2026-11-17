package at.airpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import at.airpx.workloads.account.Account;
import org.junit.jupiter.api.Test;

class AccountEntityTest {
    @Test
    void accountInitializesRequiredModelFields() {
        Account account = new Account("sonja", "sonja@example.com", "password-hash");

        assertEquals("sonja", account.getUsername());
        assertEquals("sonja@example.com", account.getEmail());
        assertNotNull(account.getCreatedAt());
    }
}
