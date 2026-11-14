package at.airpx.workloads.tag;

import java.util.List;

public interface TagRepository {
    List<Tag> findAll(Long accountId);
}
