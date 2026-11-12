package at.airpx.workloads.photo;

import at.airpx.workloads.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "photo", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "sha256_hash"}))
public class Photo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(name = "sha256_hash", nullable = false, length = 64) private String sha256;
    @Column(name = "original_filename", nullable = false) private String filename;
    @Column(name = "captured_at") private Instant capturedAt;
    @Column(nullable = false) private long size;
    @Column(name = "mime_type", nullable = false, length = 255) private String mimeType;
    @Column(name = "album_id") private Long albumId;
    @Column(name = "storage_name", nullable = false, unique = true) private String storageName;

    protected Photo() {}
    public Photo(User owner, String sha256, String filename, Instant capturedAt, long size, String mimeType, Long albumId, String storageName) {
        this.owner = owner; this.sha256 = sha256; this.filename = filename; this.capturedAt = capturedAt;
        this.size = size; this.mimeType = mimeType; this.albumId = albumId; this.storageName = storageName;
    }
    public Long getId() { return id; }
    public String getSha256() { return sha256; }
    public String getFilename() { return filename; }
    public Instant getCapturedAt() { return capturedAt; }
    public long getSize() { return size; }
    public String getMimeType() { return mimeType; }
    public Long getAlbumId() { return albumId; }
    public String getStorageName() { return storageName; }
}
