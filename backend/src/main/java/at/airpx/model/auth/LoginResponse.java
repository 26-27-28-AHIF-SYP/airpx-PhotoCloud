package at.airpx.model.auth;

public record LoginResponse(

    String accessToken,

    String tokenType,

    java.time.Instant expiresAt
) {}