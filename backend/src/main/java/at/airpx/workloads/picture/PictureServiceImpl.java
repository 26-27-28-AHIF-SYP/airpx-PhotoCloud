package at.airpx.workloads.picture;

import at.airpx.model.picture.PictureDTO;
import at.airpx.workloads.account.AccountRepository;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.album.AlbumRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@ApplicationScoped
public class PictureServiceImpl implements PictureService {
    private final PictureRepository photos;
    private final AccountRepository accounts;
    private final AlbumRepository albums;
    private final Path directory;

    public PictureServiceImpl(
            PictureRepository photos,
            AccountRepository accounts,
            AlbumRepository albums,
            @ConfigProperty(name = "photo.storage.directory", defaultValue = "./data/photos") String directory) {
        this.photos = photos;
        this.accounts = accounts;
        this.albums = albums;
        this.directory = Path.of(directory);
    }

    @Override
    public List<PictureDTO> list(long accountId) {
        return photos.findAll(accountId).stream().map(PictureServiceImpl::dto).toList();
    }

    @Override
    public List<String> missing(long accountId, List<String> hashes) {
        var existing = photos.existingHashes(
                accountId, hashes.stream().map(String::toLowerCase).toList());
        return hashes.stream()
                .map(String::toLowerCase)
                .filter(hash -> !existing.contains(hash))
                .distinct()
                .toList();
    }

    @Transactional
    @Override
    public PictureDTO upload(
            long accountId,
            FileUpload file,
            String filename,
            String capturedAt,
            String sha256,
            long declaredSize,
            String mimeType,
            String albumId)
            throws Exception {
        String hash = sha256.toLowerCase();
        if (filename == null
                || filename.isBlank()
                || mimeType == null
                || !mimeType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")
                || declaredSize < 0) throw new IllegalArgumentException("invalid metadata");
        Album targetAlbum = null;
        if (albumId != null && !albumId.isBlank()) {
            long parsedAlbumId;
            try {
                parsedAlbumId = Long.parseLong(albumId);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid album", e);
            }
            targetAlbum = albums.findByIdAndOwner(parsedAlbumId, accountId)
                    .orElseThrow(() -> new IllegalArgumentException("album is not owned by this account"));
        }
        Picture existing = photos.findByHash(accountId, hash);
        if (existing != null) {
            if (targetAlbum != null) targetAlbum.addPicture(existing);
            return dto(existing);
        }
        String storageName = UUID.randomUUID().toString();
        Files.createDirectories(directory);
        Path destination = directory.resolve(storageName);
        try {
            Files.copy(file.uploadedFile(), destination);
            if (Files.size(destination) != declaredSize) throw new IllegalArgumentException("size mismatch");
            String actualHash;
            try (InputStream input = Files.newInputStream(destination)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                actualHash = HexFormat.of().formatHex(digest.digest());
            }
            if (!actualHash.equals(hash)) throw new IllegalArgumentException("hash mismatch");
            Instant date = capturedAt == null || capturedAt.isBlank() ? null : Instant.parse(capturedAt);
            var photo = new Picture(
                    accounts.findById(accountId).orElseThrow(),
                    hash,
                    Path.of(filename).getFileName().toString(),
                    date,
                    declaredSize,
                    mimeType,
                    storageName);
            photos.persist(photo);
            if (targetAlbum != null) targetAlbum.addPicture(photo);
            return dto(photo);
        } catch (Exception e) {
            Files.deleteIfExists(destination);
            throw e;
        }
    }

    private static PictureDTO dto(Picture p) {
        return new PictureDTO(
                p.getId(), p.getSha256(), p.getFilename(), p.getCapturedAt(), p.getSize(), p.getMimeType());
    }
}
