package at.airpx.workloads.tag;

import at.airpx.workloads.account.Account;

import java.util.List;

public interface TagRepository {
    List<Tag> findAll(Long accountId);
}
