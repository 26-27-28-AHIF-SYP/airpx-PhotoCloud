package at.airpx.workloads.album;

import at.airpx.workloads.picture.Picture;
import java.util.List;
import java.util.Optional;

public interface AlbumRepository {
    Optional<Album> findByIdAndOwner(long albumId, long ownerId);

    Optional<Album> findById(long albumId);

    List<Album> findVisibleTo(long accountId);

    boolean canRename(long albumId, long accountId);

    boolean canDelete(long albumId, long accountId);

    boolean canAdd(long albumId, long accountId);

    Optional<Picture> findPictureOwnedBy(long pictureId, long ownerId);

    void delete(Album album);

    void persist(Album album);
}
