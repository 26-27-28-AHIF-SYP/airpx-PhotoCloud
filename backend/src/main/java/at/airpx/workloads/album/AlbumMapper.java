package at.airpx.workloads.album;

import at.airpx.model.album.AlbumDTO;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AlbumMapper {
    public AlbumDTO toDTO(Album album) {
        return new AlbumDTO(album.getId(), album.getName(), album.getCreatedAt());
    }
}
