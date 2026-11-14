package at.airpx.workloads.tag;

import at.airpx.model.tag.TagDTO;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TagMapper {
    public TagDTO toDTO(Tag tag) {
        return new TagDTO(tag.getId(), tag.getTagName(), tag.getDescription());
    }
}
