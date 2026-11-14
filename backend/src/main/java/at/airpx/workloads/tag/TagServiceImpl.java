package at.airpx.workloads.tag;

import at.airpx.model.tag.TagDTO;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    public List<TagDTO> getAllTags(Long accountId) {
        return tagRepository.findAll(accountId).stream().map(tagMapper::toDTO).toList();
    }
}
