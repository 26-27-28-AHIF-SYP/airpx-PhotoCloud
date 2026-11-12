package at.airpx.workloads.photo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class PhotoRepository {
    private final EntityManager em;
    public PhotoRepository(EntityManager em) { this.em = em; }
    public Photo findByHash(long userId, String hash) {
        return em.createQuery("select p from Photo p where p.owner.id = :user and p.sha256 = :hash", Photo.class)
            .setParameter("user", userId).setParameter("hash", hash).getResultStream().findFirst().orElse(null);
    }
    public Set<String> existingHashes(long userId, List<String> hashes) {
        if (hashes.isEmpty()) return Set.of();
        return Set.copyOf(em.createQuery("select p.sha256 from Photo p where p.owner.id = :user and p.sha256 in :hashes", String.class)
            .setParameter("user", userId).setParameter("hashes", hashes).getResultList());
    }
    public List<Photo> findAll(long userId) {
        return em.createQuery("select p from Photo p where p.owner.id = :user order by p.capturedAt desc nulls last, p.id", Photo.class)
            .setParameter("user", userId).getResultList();
    }
    public void persist(Photo photo) { em.persist(photo); }
}
