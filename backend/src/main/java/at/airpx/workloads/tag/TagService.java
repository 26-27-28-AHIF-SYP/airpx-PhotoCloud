package at.airpx.workloads.tag;

import at.airpx.model.tag.TagDTO;

import java.util.List;

public interface TagService {
    public List<TagDTO> getAllTags(Long accountId);
}
