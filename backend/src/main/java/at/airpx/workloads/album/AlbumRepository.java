package at.airpx.workloads.album;

import java.util.List;

public interface AlbumRepository {
    List<Album> findAllAlbums();
    boolean existsByIdAndOwner(long albumId, long ownerId);
    void persist(Album album);
}
