package at.airpx.workloads.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {

    private static final String USERNAME = "bootstrap-user";
    private static final String EMAIL = "bootstrap@example.com";
    private static final String PASSWORD = "bootstrap-password";

    @Test
    void createsAccountWhenNoUserExists() {
        InMemoryAccounts accounts = new InMemoryAccounts();

        service(accounts).bootstrap(null);

        assertEquals(1, accounts.persisted);
        assertEquals("hashed:bootstrap-password", accounts.byEmail.get(EMAIL).getPasswordHash());
    }

    @Test
    void skipsAccountWhenEmailAlreadyExists() {
        InMemoryAccounts accounts = accountsWith("other-user", EMAIL);

        service(accounts).bootstrap(null);

        assertEquals(0, accounts.persisted);
        assertEquals("other-user", accounts.byEmail.get(EMAIL).getUsername());
    }

    @Test
    void skipsAccountWhenUsernameExistsWithDifferentEmail() {
        InMemoryAccounts accounts = accountsWith(USERNAME, "existing@example.com");

        service(accounts).bootstrap(null);

        assertEquals(0, accounts.persisted);
        assertTrue(accounts.findByEmail(EMAIL).isEmpty());
    }

    @Test
    void repeatedStartupDoesNotCreateAnotherAccountOrChangePassword() {
        InMemoryAccounts accounts = new InMemoryAccounts();
        AuthServiceImpl service = service(accounts);

        service.bootstrap(null);
        service.bootstrap(null);

        assertEquals(1, accounts.persisted);
        assertTrue(service.login(EMAIL, PASSWORD).isPresent());
        assertTrue(service.login(EMAIL, "changed-password").isEmpty());
    }

    private static InMemoryAccounts accountsWith(String username, String email) {
        InMemoryAccounts accounts = new InMemoryAccounts();
        accounts.persist(new Account(username, email, "existing-hash"));
        accounts.persisted = 0;
        return accounts;
    }

    private static AuthServiceImpl service(InMemoryAccounts accounts) {
        PasswordHasher hasher = new PasswordHasher() {
            @Override
            public String hash(String password) {
                return "hashed:" + password;
            }

            @Override
            public boolean matches(String password, String encodedHash) {
                return encodedHash.equals(hash(password));
            }
        };
        return new AuthServiceImpl(
                accounts,
                hasher,
                Optional.of(USERNAME),
                Optional.of(EMAIL),
                Optional.of(PASSWORD),
                Duration.ofHours(8));
    }

    private static final class InMemoryAccounts implements AccountRepository {
        private final Map<String, Account> byEmail = new HashMap<>();
        private final Map<String, Account> byUsername = new HashMap<>();
        private int persisted;

        @Override
        public Optional<Account> findByEmail(String email) {
            return Optional.ofNullable(byEmail.get(email));
        }

        @Override
        public Optional<Account> findByUsername(String username) {
            return Optional.ofNullable(byUsername.get(username));
        }

        @Override
        public Optional<Account> findById(long id) {
            return Optional.empty();
        }

        @Override
        public void persist(Account account) {
            byEmail.put(account.getEmail(), account);
            byUsername.put(account.getUsername(), account);
            persisted++;
        }
    }
}
