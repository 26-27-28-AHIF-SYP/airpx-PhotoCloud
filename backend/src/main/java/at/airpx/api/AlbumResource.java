package at.airpx.api;

import at.airpx.model.album.AlbumRequest;
import at.airpx.model.error.ErrorResponse;
import at.airpx.workloads.album.AlbumNotFoundException;
import at.airpx.workloads.album.AlbumService;
import at.airpx.workloads.album.PictureNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.net.URI;

@Path("/api/albums")
@Produces(MediaType.APPLICATION_JSON)
public class AlbumResource {

    private final AlbumService albumService;

    public AlbumResource(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GET
    public Response getAlbums(@Context SecurityContext security) {
        return Response.ok(albumService.findAllAlbums(accountId(security))).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid @NotNull AlbumRequest request, @Context SecurityContext security) {
        var album = albumService.create(accountId(security), request.name().trim());
        return Response.created(URI.create("/api/albums/" + album.id()))
                .entity(album)
                .build();
    }

    @PUT
    @Path("/{albumId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response rename(
            @PathParam("albumId") long albumId,
            @Valid @NotNull AlbumRequest request,
            @Context SecurityContext security) {
        try {
            return Response.ok(albumService.rename(
                            accountId(security), albumId, request.name().trim()))
                    .build();
        } catch (AlbumNotFoundException exception) {
            return error(Response.Status.NOT_FOUND, "album not found");
        }
    }

    @DELETE
    @Path("/{albumId}")
    public Response delete(@PathParam("albumId") long albumId, @Context SecurityContext security) {
        try {
            albumService.delete(accountId(security), albumId);
            return Response.noContent().build();
        } catch (AlbumNotFoundException exception) {
            return error(Response.Status.NOT_FOUND, "album not found");
        }
    }

    @POST
    @Path("/{albumId}/pictures/{pictureId}")
    public Response addPicture(
            @PathParam("albumId") long albumId,
            @PathParam("pictureId") long pictureId,
            @Context SecurityContext security) {
        try {
            albumService.addPicture(accountId(security), albumId, pictureId);
            return Response.noContent().build();
        } catch (AlbumNotFoundException exception) {
            return error(Response.Status.NOT_FOUND, "album not found");
        } catch (PictureNotFoundException exception) {
            return error(Response.Status.NOT_FOUND, "picture not found");
        }
    }

    @DELETE
    @Path("/{albumId}/pictures/{pictureId}")
    public Response removePicture(
            @PathParam("albumId") long albumId,
            @PathParam("pictureId") long pictureId,
            @Context SecurityContext security) {
        try {
            albumService.removePicture(accountId(security), albumId, pictureId);
            return Response.noContent().build();
        } catch (AlbumNotFoundException exception) {
            return error(Response.Status.NOT_FOUND, "album not found");
        } catch (PictureNotFoundException exception) {
            return error(Response.Status.NOT_FOUND, "picture not found");
        }
    }

    private static long accountId(SecurityContext security) {
        return Long.parseLong(security.getUserPrincipal().getName());
    }

    private static Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
