package at.airpx.api;

import at.airpx.model.photo.HashesRequest;
import at.airpx.model.photo.PictureDTO;
import at.airpx.workloads.picture.PictureService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.util.Map;

@Path("/api/photos")
@Produces(MediaType.APPLICATION_JSON)
public class PhotoResource {
    private final PictureService photos;
    public PhotoResource(PictureService photos) { this.photos = photos; }

    @POST @Path("/missing") @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> missing(@Valid HashesRequest request, @Context SecurityContext security) {
        return Map.of("missingHashes", photos.missing(accountId(security), request.hashes()));
    }

    @GET public java.util.List<PictureDTO> list(@Context SecurityContext security) { return photos.list(accountId(security)); }

    @POST @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@RestForm("file") FileUpload file, @RestForm("filename") String filename,
            @RestForm("capturedAt") String capturedAt, @RestForm("sha256") String sha256,
            @RestForm("size") long size, @RestForm("mimeType") String mimeType,
            @RestForm("albumId") String albumId, @Context SecurityContext security) throws Exception {
        if (file == null || sha256 == null || !sha256.matches("(?i)[a-f0-9]{64}")) return Response.status(400).build();
        try {
            PictureDTO result = photos.upload(accountId(security), file, filename, capturedAt, sha256, size, mimeType, albumId);
            return Response.status(Response.Status.CREATED).entity(result).build();
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            return Response.status(400).build();
        }
    }
    private static long accountId(SecurityContext security) { return Long.parseLong(security.getUserPrincipal().getName()); }
}
