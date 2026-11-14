package at.airpx.workloads.album;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AlbumRepositoryImpl implements AlbumRepository {
    private final EntityManager entityManager;

    public AlbumRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Album> findAllAlbums() {
        return entityManager.createQuery("select a from Album a", Album.class).getResultList();
    }

    @Override
    public Optional<Album> findByIdAndOwner(long albumId, long ownerId) {
        return entityManager.createQuery(
                        "select a from Album a where a.id = :albumId and a.owner.id = :ownerId", Album.class)
                .setParameter("albumId", albumId)
                .setParameter("ownerId", ownerId)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional
    public void persist(Album album) {
        entityManager.persist(album);
    }
}
