package at.airpx.workloads.account_tag;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.picture.Picture;
import at.airpx.workloads.tag.Tag;
import jakarta.persistence.*;

@Entity
public class AccountTag {
    @EmbeddedId
    private AccountTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("accountId")
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public AccountTag() {
    }

    public AccountTag(AccountTagId id) {
        this.id = id;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public AccountTagId getId() {
        return id;
    }

    public void setId(AccountTagId id) {
        this.id = id;
    }
}
