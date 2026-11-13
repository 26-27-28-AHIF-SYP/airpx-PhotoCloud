package at.airpx.workloads.account;

import at.airpx.model.auth.LoginResponse;
import at.airpx.model.auth.RegisterResponse;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final Optional<String> bootstrapUsername;
    private final Optional<String> bootstrapEmail;
    private final Optional<String> bootstrapPassword;
    private final Duration tokenLifetime;
    private final ConcurrentHashMap<String, TokenSession> activeTokens = new ConcurrentHashMap<>();

    public AuthServiceImpl(
            AccountRepository accountRepository,
            PasswordHasher passwordHasher,
            @ConfigProperty(name = "auth.bootstrap.username") Optional<String> bootstrapUsername,
            @ConfigProperty(name = "auth.bootstrap.email") Optional<String> bootstrapEmail,
            @ConfigProperty(name = "auth.bootstrap.password") Optional<String> bootstrapPassword,
            @ConfigProperty(name = "auth.token-lifetime", defaultValue = "PT8H") Duration tokenLifetime) {
        this.accountRepository = accountRepository;
        this.passwordHasher = passwordHasher;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapPassword = bootstrapPassword;
        this.tokenLifetime = tokenLifetime;
    }

    @Transactional
    void bootstrap(@Observes StartupEvent ignored) {
        if (bootstrapUsername.isEmpty() || bootstrapEmail.isEmpty() || bootstrapPassword.isEmpty()
                || bootstrapEmail.get().isBlank() || bootstrapPassword.get().isEmpty()) {
            return;
        }

        String normalizedEmail = normalizeEmail(bootstrapEmail.get());
        if (accountRepository.findByEmail(normalizedEmail).isEmpty()) {
            Account bootstrapAccount = new Account(bootstrapUsername.get(), normalizedEmail, passwordHasher.hash(bootstrapPassword.get()));
            accountRepository.persist(bootstrapAccount);
        }
    }

    @Transactional
    @Override
    public Optional<LoginResponse> login(String email, String password) {
        Optional<Account> account = accountRepository.findByEmail(normalizeEmail(email));
        if (account.isEmpty() || !passwordHasher.matches(password, account.get().getPasswordHash())) {
            return Optional.empty();
        }

        Instant expiresAt = Instant.now().plus(tokenLifetime);
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        activeTokens.put(token, new TokenSession(account.get().getId(), expiresAt));
        return Optional.of(new LoginResponse(token, "Bearer", expiresAt));
    }

    @Override
    public boolean isValidToken(String token) {
        return authenticatedAccountId(token).isPresent();
    }

    @Override
    public Optional<Long> authenticatedAccountId(String token) {
        TokenSession session = activeTokens.get(token);
        if (session == null) return Optional.empty();
        if (session.expiresAt().isBefore(Instant.now())) {
            activeTokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.accountId());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record TokenSession(Long accountId, Instant expiresAt) {}

    @Transactional
    @Override
    public Optional<RegisterResponse> register(String username, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = username.trim();

        if (accountRepository.findByEmail(normalizedEmail).isPresent() ||
            accountRepository.findByUsername(normalizedUsername).isPresent()) {
            return Optional.empty();
        }

        Account account = new Account(username, email, passwordHasher.hash(password));

        accountRepository.persist(account);

        return Optional.of(new RegisterResponse(normalizedUsername, normalizedEmail));
    }
}
