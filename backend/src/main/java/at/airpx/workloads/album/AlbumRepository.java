package at.airpx.workloads.album;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.util.List;

@ApplicationScoped
public class AlbumRepository {

    private final EntityManager entityManager;

    public AlbumRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // --------------- Repo Methods ---------------

    public List<Album> findAllAlbums() {
        return entityManager.createQuery("select a from Album a", Album.class).getResultList();
    }
}
