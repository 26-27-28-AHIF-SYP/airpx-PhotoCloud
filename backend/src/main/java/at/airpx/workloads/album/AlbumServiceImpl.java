package at.airpx.workloads.album;

import at.airpx.model.album.AlbumDTO;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;
    private final AlbumMapper albumMapper;

    public AlbumServiceImpl(AlbumRepository albumRepository, AlbumMapper albumMapper) {
        this.albumRepository = albumRepository;
        this.albumMapper = albumMapper;
    }

    @Override
    public List<AlbumDTO> findAllAlbums() {
        return albumRepository.findAllAlbums().stream().map(albumMapper::toDTO).toList();
    }
}
