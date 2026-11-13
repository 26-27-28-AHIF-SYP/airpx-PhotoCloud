package at.airpx.workloads.account;

import at.airpx.model.auth.LoginResponse;
import at.airpx.model.auth.RegisterResponse;
import java.util.Optional;

public interface AuthService {
    Optional<LoginResponse> login(String email, String password);
    Optional<RegisterResponse> register(String username, String email, String password);
    boolean isValidToken(String token);
    Optional<Long> authenticatedAccountId(String token);
}
