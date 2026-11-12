package at.airpx.workloads.album;

import at.airpx.model.album.AlbumDTO;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AlbumService {

    private final AlbumRepository albumRepo;
    private final AlbumMapper albumMapper;

    public AlbumService(AlbumRepository albumRepository, AlbumMapper albumMapper) {
        this.albumRepo = albumRepository;
        this.albumMapper = albumMapper;
    }

    public List<AlbumDTO> findAllAlbums() {
        return albumRepo.findAllAlbums().stream().map(albumMapper::toDTO).toList();
    }
}
