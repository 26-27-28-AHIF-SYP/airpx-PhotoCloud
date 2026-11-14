package at.airpx.workloads.account;

import at.airpx.workloads.account_tag.AccountTag;
import at.airpx.workloads.contact.Contact;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 512)
    private String passwordHash;

    @OneToMany(mappedBy="account")
    private List<AccountTag> accountTags = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<Contact> outgoingContacts = new ArrayList<>();

    @OneToMany(mappedBy = "contactAccount")
    private List<Contact> incomingContacts = new ArrayList<>();

    protected Account() {
    }

    public Account(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public Long getId() { return id; }

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
}
