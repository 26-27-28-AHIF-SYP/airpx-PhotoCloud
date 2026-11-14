package at.airpx.workloads.contact;

import at.airpx.workloads.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "contact")
public class Contact {
    @EmbeddedId
    private ContactId id;

    @MapsId("accountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @MapsId("contactAccountId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_account_id", nullable = false)
    private Account contactAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactStatus status = ContactStatus.PENDING;

    protected Contact() {}

    public Contact(Account account, Account contactAccount) {
        this.account = Objects.requireNonNull(account, "account is required");
        this.contactAccount = Objects.requireNonNull(contactAccount, "contact account is required");
        if (account.getId() == null || contactAccount.getId() == null) {
            throw new IllegalArgumentException("Both accounts must be persisted before creating a contact");
        }
        if (account.getId().equals(contactAccount.getId())) {
            throw new IllegalArgumentException("An account cannot be its own contact");
        }
        this.id = new ContactId(account.getId(), contactAccount.getId());
    }

    public ContactId getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public Account getContactAccount() {
        return contactAccount;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = Objects.requireNonNull(status, "status is required");
    }
}
