package at.airpx.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    @GET
    public Response getStatus() {
        return Response.ok(Map.of("status", "up")).build();
    }
}
