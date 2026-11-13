package at.airpx.workloads.account;

public interface PasswordHasher {
    String hash(String password);
    boolean matches(String password, String encodedHash);
}
