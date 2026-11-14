package at.airpx.workloads.picture;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.picture_tag.PictureTag;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "picture", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "sha256_hash"}))
public class Picture {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "owner_id", nullable = false) private Account owner;
    @Column(name = "sha256_hash", nullable = false, length = 64) private String sha256;
    @Column(name = "original_filename", nullable = false) private String filename;
    @Column(name = "captured_at") private Instant capturedAt;
    @Column(nullable = false) private long size;
    @Column(name = "mime_type", nullable = false, length = 255) private String mimeType;
    @Column(name = "album_id") private Long albumId;
    @Column(name = "storage_name", nullable = false, unique = true) private String storageName;

    @OneToMany(mappedBy="picture")
    private List<PictureTag> pictureTags = new ArrayList<>();

    protected Picture() {}
    public Picture(Account owner, String sha256, String filename, Instant capturedAt, long size, String mimeType, Long albumId, String storageName) {
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

    public List<PictureTag> getPictureTags() {
        return pictureTags;
    }

    public void setPictureTags(List<PictureTag> pictureTags) {
        this.pictureTags = pictureTags;
    }
}
