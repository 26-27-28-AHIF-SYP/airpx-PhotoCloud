package at.airpx.api;

import at.airpx.model.tag.TagDTO;
import at.airpx.workloads.tag.TagService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/tags")
public class TagResource {
    private final TagService tags;

    public TagResource(TagService tagService) {
        this.tags = tagService;
    }

    @GET
    public List<TagDTO> list(@Context SecurityContext security) {
        return tags.getAllTags(accountId(security));
    }

    private static long accountId(SecurityContext security) {
        return Long.parseLong(security.getUserPrincipal().getName());
    }
}
