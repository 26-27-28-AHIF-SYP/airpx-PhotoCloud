package at.airpx.workloads.auth;

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
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Optional<String> bootstrapUsername;
    private final Optional<String> bootstrapEmail;
    private final Optional<String> bootstrapPassword;
    private final Duration tokenLifetime;
    private final ConcurrentHashMap<String, Instant> activeTokens = new ConcurrentHashMap<>();

    public AuthService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            @ConfigProperty(name = "auth.bootstrap.username") Optional<String> bootstrapUsername,
            @ConfigProperty(name = "auth.bootstrap.email") Optional<String> bootstrapEmail,
            @ConfigProperty(name = "auth.bootstrap.password") Optional<String> bootstrapPassword,
            @ConfigProperty(name = "auth.token-lifetime", defaultValue = "PT8H") Duration tokenLifetime) {
        this.userRepository = userRepository;
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
        if (userRepository.findByEmail(normalizedEmail).isEmpty()) {
            User bootstrapUser = new User(bootstrapUsername.get(), normalizedEmail, passwordHasher.hash(bootstrapPassword.get()));
            userRepository.persist(bootstrapUser);
        }
    }

    @Transactional
    public Optional<LoginResponse> login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(email));
        if (user.isEmpty() || !passwordHasher.matches(password, user.get().getPasswordHash())) {
            return Optional.empty();
        }

        Instant expiresAt = Instant.now().plus(tokenLifetime);
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        activeTokens.put(token, expiresAt);
        return Optional.of(new LoginResponse(token, "Bearer", expiresAt));
    }

    public boolean isValidToken(String token) {
        Instant expiresAt = activeTokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            activeTokens.remove(token);
            return false;
        }
        return true;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Transactional
    public Optional<RegisterResponse> register(String username, String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = username.trim();

        if (userRepository.findByEmail(normalizedEmail).isPresent() ||
            userRepository.findByUsername(normalizedUsername).isPresent()) {
            return Optional.empty();
        }

        User user = new User(username, email, passwordHasher.hash(password));

        userRepository.persist(user);

        return Optional.of(new RegisterResponse(normalizedUsername, normalizedEmail));
    }
}
