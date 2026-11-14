package at.airpx.workloads.account_tag;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AccountTagId implements Serializable {
    private Long accountId;
    private Integer tagId;

    public AccountTagId() {}

    public AccountTagId(Long accountId, Integer tagId) {
        this.accountId = accountId;
        this.tagId = tagId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long pictureId) {
        this.accountId = pictureId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AccountTagId that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, tagId);
    }
}
