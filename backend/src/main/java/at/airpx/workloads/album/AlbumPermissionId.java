package at.airpx.workloads.album;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AlbumPermissionId implements Serializable {
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "album_id")
    private Long albumId;

    protected AlbumPermissionId() {}

    public AlbumPermissionId(Long accountId, Long albumId) {
        this.accountId = accountId;
        this.albumId = albumId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getAlbumId() {
        return albumId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AlbumPermissionId that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(albumId, that.albumId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, albumId);
    }
}
