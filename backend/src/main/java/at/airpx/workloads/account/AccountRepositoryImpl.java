package at.airpx.workloads.account;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.Optional;

@ApplicationScoped
public class AccountRepositoryImpl implements AccountRepository {

    private final EntityManager entityManager;

    public AccountRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Account> findByEmail(String email) {
        return entityManager.createQuery(
                        "select u from Account u where u.email = :email", Account.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Override
    public void persist(Account account) {
        entityManager.persist(account);
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return entityManager.createQuery(
                        "select u from Account u where u.username = :username", Account.class)
            .setParameter("username", username)
            .getResultStream()
            .findFirst();
    }

    @Override
    public Optional<Account> findById(long id) { return Optional.ofNullable(entityManager.find(Account.class, id)); }
}
