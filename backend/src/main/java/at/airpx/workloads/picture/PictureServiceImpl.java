package at.airpx.workloads.picture;

import at.airpx.model.picture.PictureDTO;
import at.airpx.workloads.account.AccountRepository;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.album.AlbumRepository;
import io.quarkus.runtime.configuration.MemorySize;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@ApplicationScoped
public class PictureServiceImpl implements PictureService {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String WEBP_MIME_TYPE = "image/webp";
    private static final int HASH_BUFFER_SIZE = 64 * 1024;
    private static final int MIME_HEADER_LENGTH = 12;
    private static final int WEBP_SIGNATURE_OFFSET = 8;

    private final PictureRepository pictures;
    private final AccountRepository accounts;
    private final AlbumRepository albums;
    private final PictureMapper mapper;
    private final Path directory;
    private final long maxSize;
    private final int pageSizeMax;

    public PictureServiceImpl(
            PictureRepository pictures,
            AccountRepository accounts,
            AlbumRepository albums,
            PictureMapper mapper,
            @ConfigProperty(name = "picture.storage.directory", defaultValue = "./data/photos") String directory,
            @ConfigProperty(name = "picture.max-size", defaultValue = "25M") MemorySize maxSize,
            @ConfigProperty(name = "picture.page-size-max", defaultValue = "100") int pageSizeMax) {
        this.pictures = pictures;
        this.accounts = accounts;
        this.albums = albums;
        this.mapper = mapper;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
        this.maxSize = maxSize.asLongValue();
        this.pageSizeMax = Math.max(1, pageSizeMax);
    }

    @Override
    public PicturePage list(long accountId, int page, int size) {
        if (page < 1 || size < 1) throw new IllegalArgumentException("page and size must be positive");
        int effectiveSize = Math.min(size, pageSizeMax);
        long total = pictures.countByOwnerId(accountId);
        long offset = (long) (page - 1) * effectiveSize;
        if (offset > Integer.MAX_VALUE) return new PicturePage(List.of(), page, effectiveSize, total);
        List<PictureDTO> content = pictures.findByOwnerId(accountId, (int) offset, effectiveSize).stream()
                .map(mapper::toDTO)
                .toList();
        return new PicturePage(content, page, effectiveSize, total);
    }

    @Override
    public List<String> missing(long accountId, List<String> hashes) {
        List<String> normalized =
                hashes.stream().map(hash -> hash.toLowerCase(Locale.ROOT)).toList();
        var existing = pictures.existingHashes(accountId, normalized);
        return normalized.stream()
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
            Long declaredSize,
            String mimeType,
            String albumId) {
        if (file == null) throw new IllegalArgumentException("file is required");
        Path uploadedFile = file.uploadedFile();
        long actualSize = sizeOf(uploadedFile);
        if (actualSize == 0) throw new IllegalArgumentException("file is empty");
        if (actualSize > maxSize) {
            throw new PictureTooLargeException("picture exceeds the maximum size of " + maxSize + " bytes");
        }

        String detectedMimeType = detectMimeType(uploadedFile);
        if (detectedMimeType == null) {
            throw new UnsupportedPictureTypeException("only jpeg, png and webp images are supported");
        }
        if (mimeType != null
                && !mimeType.isBlank()
                && !detectedMimeType.equals(mimeType.strip().toLowerCase(Locale.ROOT))) {
            throw new UnsupportedPictureTypeException("declared mime type does not match the image content");
        }
        if (declaredSize != null && declaredSize != actualSize) {
            throw new IllegalArgumentException("size mismatch");
        }

        String hash = hash(uploadedFile);
        if (sha256 != null
                && !sha256.isBlank()
                && !MessageDigest.isEqual(
                        hash.getBytes(StandardCharsets.UTF_8),
                        sha256.strip().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("hash mismatch");
        }

        Album targetAlbum = findTargetAlbum(accountId, albumId);
        Picture existing = pictures.findByHash(accountId, hash);
        if (existing != null) {
            if (targetAlbum != null) targetAlbum.addPicture(existing);
            return mapper.toDTO(existing);
        }

        String storageName = UUID.randomUUID().toString();
        Path destination = directory.resolve(storageName);
        try {
            Files.createDirectories(directory);
            Files.copy(uploadedFile, destination);
        } catch (IOException exception) {
            deleteQuietly(destination, exception);
            throw new UncheckedIOException("could not store picture", exception);
        }

        try {
            Picture picture = new Picture(
                    accounts.findById(accountId)
                            .orElseThrow(() -> new IllegalStateException("authenticated account not found")),
                    hash,
                    baseName(filename, file.fileName()),
                    parseCapturedAt(capturedAt),
                    actualSize,
                    detectedMimeType,
                    storageName);
            pictures.persist(picture);
            if (targetAlbum != null) targetAlbum.addPicture(picture);
            return mapper.toDTO(picture);
        } catch (RuntimeException exception) {
            deleteQuietly(destination, exception);
            throw exception;
        }
    }

    @Override
    public Optional<PictureFile> load(long accountId, long pictureId) {
        return pictures.findById(pictureId)
                .filter(picture -> isOwnedBy(picture, accountId) || pictures.canRead(pictureId, accountId))
                .map(picture -> new PictureFile(storagePath(picture.getStorageName()), picture.getMimeType()))
                .filter(file -> Files.isRegularFile(file.path()));
    }

    private static boolean isOwnedBy(Picture picture, long accountId) {
        return picture.getOwner().getId() == accountId;
    }

    private Path storagePath(String storageName) {
        Path path = directory.resolve(storageName).normalize();
        if (!path.startsWith(directory)) throw new IllegalStateException("invalid storage name");
        return path;
    }

    private Album findTargetAlbum(long accountId, String albumId) {
        if (albumId == null || albumId.isBlank()) return null;
        long parsedAlbumId;
        try {
            parsedAlbumId = Long.parseLong(albumId.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid album", exception);
        }
        return albums.findByIdAndOwner(parsedAlbumId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("album is not owned by this account"));
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("could not read upload", exception);
        }
    }

    private static String hash(Path file) {
        try (InputStream input = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[HASH_BUFFER_SIZE];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException exception) {
            throw new UncheckedIOException("could not read upload", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String detectMimeType(Path file) {
        byte[] header = new byte[MIME_HEADER_LENGTH];
        int read;
        try (InputStream input = Files.newInputStream(file)) {
            read = input.readNBytes(header, 0, header.length);
        } catch (IOException exception) {
            throw new UncheckedIOException("could not read upload", exception);
        }
        if (startsWith(header, read, PNG_SIGNATURE)) return PNG_MIME_TYPE;
        if (startsWith(header, read, JPEG_SIGNATURE)) return JPEG_MIME_TYPE;
        if (read >= MIME_HEADER_LENGTH
                && startsWith(header, read, RIFF_SIGNATURE)
                && startsAt(header, WEBP_SIGNATURE_OFFSET, WEBP_SIGNATURE)) return WEBP_MIME_TYPE;
        return null;
    }

    private static boolean startsWith(byte[] data, int length, byte[] prefix) {
        return length >= prefix.length && startsAt(data, 0, prefix);
    }

    private static boolean startsAt(byte[] data, int offset, byte[] expected) {
        if (data.length < offset + expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if (data[offset + index] != expected[index]) return false;
        }
        return true;
    }

    private static String baseName(String filename, String uploadedFileName) {
        String candidate = filename == null || filename.isBlank() ? uploadedFileName : filename;
        if (candidate == null || candidate.isBlank()) return "upload";
        Path name = Path.of(candidate.strip()).getFileName();
        if (name == null) throw new IllegalArgumentException("invalid filename");
        String base = name.toString().strip();
        if (base.isEmpty() || base.equals(".") || base.equals("..")) {
            throw new IllegalArgumentException("invalid filename");
        }
        return base;
    }

    private static Instant parseCapturedAt(String capturedAt) {
        if (capturedAt == null || capturedAt.isBlank()) return null;
        try {
            return Instant.parse(capturedAt.strip());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid capturedAt", exception);
        }
    }

    private static void deleteQuietly(Path path, Exception failure) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
