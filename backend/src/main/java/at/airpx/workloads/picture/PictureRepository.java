package at.airpx.workloads.picture;

import java.util.List;
import java.util.Set;

public interface PictureRepository {
    Picture findByHash(long accountId, String hash);

    Set<String> existingHashes(long accountId, List<String> hashes);

    List<Picture> findAll(long accountId);

    void persist(Picture picture);
}
