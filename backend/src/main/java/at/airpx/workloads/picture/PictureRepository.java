package at.airpx.workloads.picture;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PictureRepository {
    Picture findByHash(long accountId, String hash);

    Set<String> existingHashes(long accountId, List<String> hashes);

    List<Picture> findByOwnerId(long ownerId, int offset, int limit);

    long countByOwnerId(long ownerId);

    Optional<Picture> findById(long pictureId);

    boolean canRead(long pictureId, long accountId);

    void persist(Picture picture);
}
