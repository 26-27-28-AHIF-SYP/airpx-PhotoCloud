package at.airpx.workloads.album;

import at.airpx.model.album.AlbumDTO;
import java.util.List;

public interface AlbumService {
    List<AlbumDTO> findAllAlbums();
}
