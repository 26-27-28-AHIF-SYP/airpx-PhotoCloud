package at.airpx.workloads.album;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.picture.Picture;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Album {

    @Id
    @SequenceGenerator(name = "album_sequence", sequenceName = "album_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "album_sequence")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Account owner;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private boolean isPublicOnProfile;

    @Column(name = "qr_code_token", length = 256)
    private String qrcodeToken;

    @ManyToMany
    @JoinTable(
            name = "album_pictures",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "picture_id"))
    private List<Picture> pictures = new ArrayList<>();

    @OneToMany(mappedBy = "album")
    private List<AlbumPermission> permissions = new ArrayList<>();

    @OneToMany(mappedBy = "album")
    private List<AlbumShareLink> shareLinks = new ArrayList<>();

    public Album() {}

    public Album(String name, Account owner) {
        this.name = name;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Account getOwner() {
        return owner;
    }

    public void setOwner(Account owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublicOnProfile() {
        return isPublicOnProfile;
    }

    public void setPublicOnProfile(boolean publicOnProfile) {
        isPublicOnProfile = publicOnProfile;
    }

    public String getQrcodeToken() {
        return qrcodeToken;
    }

    public void setQrcodeToken(String qrcodeToken) {
        this.qrcodeToken = qrcodeToken;
    }

    public List<Picture> getPictures() {
        return pictures;
    }

    public List<AlbumPermission> getPermissions() {
        return permissions;
    }

    public List<AlbumShareLink> getShareLinks() {
        return shareLinks;
    }

    public void addPicture(Picture picture) {
        if (pictures.stream()
                .noneMatch(existing -> existing == picture
                        || (existing.getId() != null && Objects.equals(existing.getId(), picture.getId())))) {
            pictures.add(picture);
            picture.getAlbums().add(this);
        }
    }

    public void removePicture(Picture picture) {
        pictures.removeIf(existing ->
                existing == picture || (picture.getId() != null && Objects.equals(existing.getId(), picture.getId())));
        picture.getAlbums()
                .removeIf(existing -> existing == this || (id != null && Objects.equals(existing.getId(), id)));
    }
}
