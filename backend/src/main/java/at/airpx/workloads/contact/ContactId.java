package at.airpx.workloads.contact;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContactId implements Serializable {
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "contact_account_id")
    private Long contactAccountId;

    public ContactId() {
    }

    public ContactId(Long accountId, Long contactAccountId) {
        this.accountId = accountId;
        this.contactAccountId = contactAccountId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getContactAccountId() {
        return contactAccountId;
    }

    public void setContactAccountId(Long contactAccountId) {
        this.contactAccountId = contactAccountId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ContactId contactId)) return false;
        return Objects.equals(accountId, contactId.accountId) && Objects.equals(contactAccountId, contactId.contactAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, contactAccountId);
    }
}
