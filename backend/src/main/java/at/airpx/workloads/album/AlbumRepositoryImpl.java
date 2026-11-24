package at.airpx.workloads.album;

import at.airpx.workloads.picture.Picture;
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
    public Optional<Album> findById(long albumId) {
        return Optional.ofNullable(entityManager.find(Album.class, albumId));
    }

    @Override
    public Optional<Album> findByIdAndOwner(long albumId, long ownerId) {
        return entityManager
                .createQuery("select a from Album a where a.id = :albumId and a.owner.id = :ownerId", Album.class)
                .setParameter("albumId", albumId)
                .setParameter("ownerId", ownerId)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Album> findVisibleTo(long accountId) {
        return entityManager
                .createQuery("""
                        select distinct a from Album a
                        left join a.permissions permission
                        where a.owner.id = :account or (permission.account.id = :account and
                          permission.canRead = true)
                        order by a.createdAt desc
                        """, Album.class)
                .setParameter("account", accountId)
                .getResultList();
    }

    @Override
    public boolean canRename(long albumId, long accountId) {
        return hasPermission(albumId, accountId, "canRename");
    }

    @Override
    public boolean canDelete(long albumId, long accountId) {
        return hasPermission(albumId, accountId, "canDelete");
    }

    @Override
    public boolean canAdd(long albumId, long accountId) {
        return hasPermission(albumId, accountId, "canAdd");
    }

    private boolean hasPermission(long albumId, long accountId, String permission) {
        Long count = entityManager
                .createQuery(
                        "select count(a) from Album a left join a.permissions p "
                                + "where a.id = :album and (a.owner.id = :account or (p.account.id = :account and p."
                                + permission + " = true))",
                        Long.class)
                .setParameter("album", albumId)
                .setParameter("account", accountId)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Optional<Picture> findPictureOwnedBy(long pictureId, long ownerId) {
        return entityManager
                .createQuery("select p from Picture p where p.id = :picture and p.owner.id = :owner", Picture.class)
                .setParameter("picture", pictureId)
                .setParameter("owner", ownerId)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional
    public void delete(Album album) {
        entityManager
                .createQuery("delete from AlbumPermission p where p.album.id = :albumId")
                .setParameter("albumId", album.getId())
                .executeUpdate();
        entityManager
                .createQuery("delete from AlbumShareLink s where s.album.id = :albumId")
                .setParameter("albumId", album.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.remove(album);
    }

    @Override
    @Transactional
    public void persist(Album album) {
        entityManager.persist(album);
    }
}
