package at.airpx.workloads.album;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository {
    List<Album> findAllAlbums();
    Optional<Album> findByIdAndOwner(long albumId, long ownerId);
    void persist(Album album);
}
