package at.airpx.workloads.album;

import at.airpx.model.album.AlbumDTO;
import java.util.List;

public interface AlbumService {
    List<AlbumDTO> findAllAlbums(long accountId);

    AlbumDTO create(long accountId, String name);

    AlbumDTO rename(long accountId, long albumId, String name);

    void delete(long accountId, long albumId);

    void addPicture(long accountId, long albumId, long pictureId);

    void removePicture(long accountId, long albumId, long pictureId);
}
