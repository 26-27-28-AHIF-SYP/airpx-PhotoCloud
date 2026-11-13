package at.airpx.workloads.picture;

import java.util.List;
import java.util.Set;

public interface PictureRepository {
    Picture findByHash(long userId, String hash);
    Set<String> existingHashes(long userId, List<String> hashes);
    List<Picture> findAll(long userId);
    void persist(Picture picture);
}
