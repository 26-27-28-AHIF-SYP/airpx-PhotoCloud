package at.airpx.api;

import at.airpx.workloads.album.AlbumRepository;
import at.airpx.workloads.album.AlbumService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/albums")
@Produces(MediaType.APPLICATION_JSON)
public class AlbumResource {

    private final AlbumService albumService;

    public AlbumResource(AlbumService albumService) { this.albumService = albumService; }

    @GET
    public Response getAlbums() {
        return Response.ok(albumService.findAllAlbums()).build();
    }
}
