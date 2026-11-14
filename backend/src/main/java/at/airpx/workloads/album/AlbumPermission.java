package at.airpx.workloads.album;

import at.airpx.workloads.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "album_permission")
public class AlbumPermission {
    @EmbeddedId
    private AlbumPermissionId id;

    @MapsId("accountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @MapsId("albumId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @Column(nullable = false)
    private boolean canRead;

    @Column(nullable = false)
    private boolean canAdd;

    @Column(nullable = false)
    private boolean canDelete;

    @Column(nullable = false)
    private boolean canRename;

    protected AlbumPermission() {}

    public AlbumPermission(
            Account account, Album album, boolean canRead, boolean canAdd, boolean canDelete, boolean canRename) {
        this.account = account;
        this.album = album;
        this.id = new AlbumPermissionId(account.getId(), album.getId());
        this.canRead = canRead;
        this.canAdd = canAdd;
        this.canDelete = canDelete;
        this.canRename = canRename;
    }

    public AlbumPermissionId getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Album getAlbum() {
        return album;
    }

    public boolean canRead() {
        return canRead;
    }

    public boolean canAdd() {
        return canAdd;
    }

    public boolean canDelete() {
        return canDelete;
    }

    public boolean canRename() {
        return canRename;
    }
}
