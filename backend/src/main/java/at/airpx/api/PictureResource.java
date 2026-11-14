package at.airpx.api;

import at.airpx.model.error.ErrorResponse;
import at.airpx.model.picture.HashesRequest;
import at.airpx.model.picture.PictureDTO;
import at.airpx.workloads.picture.PicturePage;
import at.airpx.workloads.picture.PictureService;
import at.airpx.workloads.picture.PictureTooLargeException;
import at.airpx.workloads.picture.UnsupportedPictureTypeException;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/pictures")
@Produces(MediaType.APPLICATION_JSON)
public class PictureResource {
    private static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final String PAGE_HEADER = "X-Page";
    private static final String PAGE_SIZE_HEADER = "X-Page-Size";

    private final PictureService pictures;

    public PictureResource(PictureService pictures) {
        this.pictures = pictures;
    }

    @POST
    @Path("/missing")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> missing(@Valid HashesRequest request, @Context SecurityContext security) {
        return Map.of("missingHashes", pictures.missing(accountId(security), request.hashes()));
    }

    @GET
    public Response list(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @Context SecurityContext security) {
        if (page < 1 || size < 1) {
            return error(Response.Status.BAD_REQUEST, "page and size must be positive");
        }
        PicturePage result = pictures.list(accountId(security), page, size);
        return Response.ok(result.pictures())
                .header(TOTAL_COUNT_HEADER, result.total())
                .header(PAGE_HEADER, result.page())
                .header(PAGE_SIZE_HEADER, result.size())
                .build();
    }

    @GET
    @Path("/{pictureId}")
    @Produces({MediaType.APPLICATION_JSON, "image/jpeg", "image/png", "image/webp"})
    public Response load(@PathParam("pictureId") long pictureId, @Context SecurityContext security) {
        return pictures.load(accountId(security), pictureId)
                .map(file -> Response.ok((StreamingOutput) output -> {
                            try (InputStream input = Files.newInputStream(file.path())) {
                                input.transferTo(output);
                            }
                        })
                        .type(file.mimeType())
                        .build())
                .orElseGet(() -> error(Response.Status.NOT_FOUND, "picture not found"));
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(
            @RestForm("file") FileUpload file,
            @RestForm("filename") String filename,
            @RestForm("capturedAt") String capturedAt,
            @RestForm("sha256") String sha256,
            @RestForm("size") Long size,
            @RestForm("mimeType") String mimeType,
            @RestForm("albumId") String albumId,
            @Context SecurityContext security,
            @Context UriInfo uriInfo) {
        try {
            PictureDTO result =
                    pictures.upload(accountId(security), file, filename, capturedAt, sha256, size, mimeType, albumId);
            URI location = uriInfo.getAbsolutePathBuilder()
                    .path(String.valueOf(result.id()))
                    .build();
            return Response.created(location).entity(result).build();
        } catch (PictureTooLargeException exception) {
            return error(Response.Status.REQUEST_ENTITY_TOO_LARGE, exception.getMessage());
        } catch (UnsupportedPictureTypeException exception) {
            return error(Response.Status.UNSUPPORTED_MEDIA_TYPE, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return error(Response.Status.BAD_REQUEST, exception.getMessage());
        }
    }

    private static Response error(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private static long accountId(SecurityContext security) {
        return Long.parseLong(security.getUserPrincipal().getName());
    }
}
