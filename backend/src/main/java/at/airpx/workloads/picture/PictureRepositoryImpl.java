package at.airpx.workloads.picture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class PictureRepositoryImpl implements PictureRepository {
    private final EntityManager em;

    public PictureRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public Picture findByHash(long accountId, String hash) {
        return em.createQuery("select p from Picture p where p.owner.id = :account and p.sha256 = :hash", Picture.class)
                .setParameter("account", accountId)
                .setParameter("hash", hash)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<String> existingHashes(long accountId, List<String> hashes) {
        if (hashes.isEmpty()) return Set.of();
        return Set.copyOf(em.createQuery(
                        "select p.sha256 from Picture p where p.owner.id = :account and p.sha256 in :hashes",
                        String.class)
                .setParameter("account", accountId)
                .setParameter("hashes", hashes)
                .getResultList());
    }

    @Override
    public List<Picture> findAll(long accountId) {
        return em.createQuery(
                        "select p from Picture p where p.owner.id = :account order by p.capturedAt desc nulls last, p.id",
                        Picture.class)
                .setParameter("account", accountId)
                .getResultList();
    }

    @Override
    public void persist(Picture picture) {
        em.persist(picture);
    }
}
