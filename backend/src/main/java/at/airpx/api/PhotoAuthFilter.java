package at.airpx.api;

import at.airpx.workloads.auth.AuthService;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class PhotoAuthFilter implements ContainerRequestFilter {
    private final AuthService authService;

    public PhotoAuthFilter(AuthService authService) { this.authService = authService; }

    @Override
    public void filter(ContainerRequestContext request) {
        if (!request.getUriInfo().getPath().replaceFirst("^/", "").startsWith("api/photos")) return;
        String header = request.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            request.abortWith(Response.status(401).build());
            return;
        }
        var userId = authService.authenticatedUserId(header.substring(7));
        if (userId.isEmpty()) {
            request.abortWith(Response.status(401).build());
            return;
        }
        boolean secure = request.getSecurityContext().isSecure();
        request.setSecurityContext(new jakarta.ws.rs.core.SecurityContext() {
            @Override public Principal getUserPrincipal() { return () -> userId.get().toString(); }
            @Override public boolean isUserInRole(String role) { return false; }
            @Override public boolean isSecure() { return secure; }
            @Override public String getAuthenticationScheme() { return "Bearer"; }
        });
    }
}
