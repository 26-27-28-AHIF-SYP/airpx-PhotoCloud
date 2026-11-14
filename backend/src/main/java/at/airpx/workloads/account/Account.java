package at.airpx.workloads.account;

import at.airpx.workloads.account_tag.AccountTag;
import at.airpx.workloads.contact.Contact;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.album.AlbumPermission;
import at.airpx.workloads.picture.Picture;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 512)
    private String passwordHash;

    @Column(nullable = false)
    private boolean twoFactorEnabled;

    @Column(unique = true, length = 150)
    private String ssoId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 150)
    private String twoFactorSecret;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_picture_id")
    private Picture profilePicture;

    @OneToMany(mappedBy="account")
    private List<AccountTag> accountTags = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<Contact> outgoingContacts = new ArrayList<>();

    @OneToMany(mappedBy = "contactAccount")
    private List<Contact> incomingContacts = new ArrayList<>();

    @OneToMany(mappedBy = "owner")
    private List<Album> albums = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<AlbumPermission> albumPermissions = new ArrayList<>();

    protected Account() {
    }

    public Account(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getUsername() { return username; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getEmail() {
        return email;
    }

    public Long getId() { return id; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getSsoId() { return ssoId; }

    public void setSsoId(String ssoId) { this.ssoId = ssoId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getTwoFactorSecret() { return twoFactorSecret; }

    public void setTwoFactorSecret(String twoFactorSecret) { this.twoFactorSecret = twoFactorSecret; }

    public Picture getProfilePicture() { return profilePicture; }

    public void setProfilePicture(Picture profilePicture) { this.profilePicture = profilePicture; }

    public String getPasswordHash() {
        return passwordHash;
    }

    public List<AccountTag> getAccountTags() {
        return accountTags;
    }

    public void setAccountTags(List<AccountTag> accountTags) {
        this.accountTags = accountTags;
    }

    public List<Contact> getOutgoingContacts() {
        return outgoingContacts;
    }

    public List<Contact> getIncomingContacts() {
        return incomingContacts;
    }

    public List<Album> getAlbums() { return albums; }

    public List<AlbumPermission> getAlbumPermissions() { return albumPermissions; }
}
