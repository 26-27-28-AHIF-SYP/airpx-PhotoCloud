package at.airpx.workloads.picture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
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
    public List<Picture> findByOwnerId(long ownerId, int offset, int limit) {
        return em.createQuery(
                        "select p from Picture p where p.owner.id = :owner "
                                + "order by p.capturedAt desc nulls last, p.id",
                        Picture.class)
                .setParameter("owner", ownerId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public long countByOwnerId(long ownerId) {
        return em.createQuery("select count(p) from Picture p where p.owner.id = :owner", Long.class)
                .setParameter("owner", ownerId)
                .getSingleResult();
    }

    @Override
    public Optional<Picture> findById(long pictureId) {
        return Optional.ofNullable(em.find(Picture.class, pictureId));
    }

    @Override
    public boolean canRead(long pictureId, long accountId) {
        Long count = em.createQuery("""
                        select count(permission) from AlbumPermission permission
                        join permission.album album
                        join album.pictures picture
                        where permission.account.id = :account
                          and permission.canRead = true
                          and picture.id = :picture
                        """, Long.class)
                .setParameter("account", accountId)
                .setParameter("picture", pictureId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public void persist(Picture picture) {
        em.persist(picture);
    }
}
