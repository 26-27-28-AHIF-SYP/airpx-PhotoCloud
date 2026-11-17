package at.airpx.workloads.account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByEmail(String email);

    Optional<Account> findByUsername(String username);

    Optional<Account> findById(long id);

    void persist(Account account);
}
