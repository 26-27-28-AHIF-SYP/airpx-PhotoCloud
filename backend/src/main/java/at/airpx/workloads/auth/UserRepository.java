package at.airpx.workloads.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.Optional;

@ApplicationScoped
public class UserRepository {

    private final EntityManager entityManager;

    public UserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<User> findByEmail(String email) {
        return entityManager.createQuery(
                        "select u from User u where u.email = :email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    public void persist(User user) {
        entityManager.persist(user);
    }

    public Optional<User> findByUsername(String username) {
        return entityManager.createQuery(
                        "select u from User u where u.username = :username", User.class)
            .setParameter("username", username)
            .getResultStream()
            .findFirst();
    }

    public Optional<User> findById(long id) { return Optional.ofNullable(entityManager.find(User.class, id)); }
}
