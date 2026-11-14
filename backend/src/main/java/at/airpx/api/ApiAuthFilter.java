package at.airpx.api;

import at.airpx.workloads.account.AuthService;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiAuthFilter implements ContainerRequestFilter {
    private static final Set<String> PROTECTED_PATHS = Set.of("api/pictures");

    private final AuthService authService;

    public ApiAuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void filter(ContainerRequestContext request) {
        String requestPath = request.getUriInfo().getPath();
        String path = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        boolean protectedPath = PROTECTED_PATHS.stream()
                .anyMatch(base -> path.equals(base) || path.startsWith(base + "/"));
        if (!protectedPath) return;

        String header = request.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() == 7) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        var accountId = authService.authenticatedAccountId(header.substring(7));
        if (accountId.isEmpty()) {
            request.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        boolean secure = request.getSecurityContext().isSecure();
        request.setSecurityContext(new jakarta.ws.rs.core.SecurityContext() {
            @Override public Principal getUserPrincipal() { return () -> accountId.get().toString(); }
            @Override public boolean isUserInRole(String role) { return false; }
            @Override public boolean isSecure() { return secure; }
            @Override public String getAuthenticationScheme() { return "Bearer"; }
        });
    }
}
