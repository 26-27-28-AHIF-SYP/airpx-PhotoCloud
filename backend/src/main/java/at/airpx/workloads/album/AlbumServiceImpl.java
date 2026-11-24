package at.airpx.workloads.album;

import at.airpx.model.album.AlbumDTO;
import at.airpx.workloads.account.AccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class AlbumServiceImpl implements AlbumService {
    private final AlbumRepository albumRepository;
    private final AlbumMapper albumMapper;
    private final AccountRepository accountRepository;

    public AlbumServiceImpl(
            AlbumRepository albumRepository, AlbumMapper albumMapper, AccountRepository accountRepository) {
        this.albumRepository = albumRepository;
        this.albumMapper = albumMapper;
        this.accountRepository = accountRepository;
    }

    @Override
    public List<AlbumDTO> findAllAlbums(long accountId) {
        return albumRepository.findVisibleTo(accountId).stream()
                .map(albumMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public AlbumDTO create(long accountId, String name) {
        var owner = accountRepository.findById(accountId).orElseThrow(AlbumNotFoundException::new);
        Album album = new Album(name, owner);
        albumRepository.persist(album);
        return albumMapper.toDTO(album);
    }

    @Override
    @Transactional
    public AlbumDTO rename(long accountId, long albumId, String name) {
        if (!albumRepository.canRename(albumId, accountId)) throw new AlbumNotFoundException();
        Album album = albumRepository.findById(albumId).orElseThrow(AlbumNotFoundException::new);
        album.setName(name);
        return albumMapper.toDTO(album);
    }

    @Override
    @Transactional
    public void delete(long accountId, long albumId) {
        if (!albumRepository.canDelete(albumId, accountId)) throw new AlbumNotFoundException();
        albumRepository.delete(albumRepository.findById(albumId).orElseThrow(AlbumNotFoundException::new));
    }

    @Override
    @Transactional
    public void addPicture(long accountId, long albumId, long pictureId) {
        if (!albumRepository.canAdd(albumId, accountId)) throw new AlbumNotFoundException();
        Album album = albumRepository.findById(albumId).orElseThrow(AlbumNotFoundException::new);
        var picture =
                albumRepository.findPictureOwnedBy(pictureId, accountId).orElseThrow(PictureNotFoundException::new);
        album.addPicture(picture);
    }

    @Override
    @Transactional
    public void removePicture(long accountId, long albumId, long pictureId) {
        if (!albumRepository.canAdd(albumId, accountId)) throw new AlbumNotFoundException();
        Album album = albumRepository.findById(albumId).orElseThrow(AlbumNotFoundException::new);
        var picture =
                albumRepository.findPictureOwnedBy(pictureId, accountId).orElseThrow(PictureNotFoundException::new);
        album.removePicture(picture);
    }
}
