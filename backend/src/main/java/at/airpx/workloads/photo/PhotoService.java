package at.airpx.workloads.photo;

import at.airpx.model.photo.PhotoDTO;
import at.airpx.workloads.album.AlbumRepository;
import at.airpx.workloads.auth.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PhotoService {
    private final PhotoRepository photos;
    private final UserRepository users;
    private final AlbumRepository albums;
    private final Path directory;

    public PhotoService(PhotoRepository photos, UserRepository users, AlbumRepository albums,
            @ConfigProperty(name = "photo.storage.directory", defaultValue = "./data/photos") String directory) {
        this.photos = photos; this.users = users; this.albums = albums; this.directory = Path.of(directory);
    }

    public List<PhotoDTO> list(long userId) { return photos.findAll(userId).stream().map(PhotoService::dto).toList(); }
    public List<String> missing(long userId, List<String> hashes) {
        var existing = photos.existingHashes(userId, hashes.stream().map(String::toLowerCase).toList());
        return hashes.stream().map(String::toLowerCase).filter(hash -> !existing.contains(hash)).distinct().toList();
    }

    @Transactional
    public PhotoDTO upload(long userId, FileUpload file, String filename, String capturedAt, String sha256,
                           long declaredSize, String mimeType, String albumId) throws Exception {
        String hash = sha256.toLowerCase();
        Photo existing = photos.findByHash(userId, hash);
        if (existing != null) return dto(existing);
        if (filename == null || filename.isBlank() || mimeType == null
                || !mimeType.toLowerCase(java.util.Locale.ROOT).startsWith("image/") || declaredSize < 0)
            throw new IllegalArgumentException("invalid metadata");
        Long album = null;
        if (albumId != null && !albumId.isBlank()) {
            try { album = Long.valueOf(albumId); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("invalid album", e); }
            if (!albums.existsByIdAndOwner(album, userId)) throw new IllegalArgumentException("album is not owned by user");
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
                byte[] buffer = new byte[64 * 1024]; int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                actualHash = HexFormat.of().formatHex(digest.digest());
            }
            if (!actualHash.equals(hash)) throw new IllegalArgumentException("hash mismatch");
            Instant date = capturedAt == null || capturedAt.isBlank() ? null : Instant.parse(capturedAt);
            var photo = new Photo(users.findById(userId).orElseThrow(), hash, Path.of(filename).getFileName().toString(),
                date, declaredSize, mimeType, album, storageName);
            photos.persist(photo);
            return dto(photo);
        } catch (Exception e) {
            Files.deleteIfExists(destination);
            throw e;
        }
    }
    private static PhotoDTO dto(Photo p) { return new PhotoDTO(p.getId(), p.getSha256(), p.getFilename(), p.getCapturedAt(), p.getSize(), p.getMimeType()); }
}
