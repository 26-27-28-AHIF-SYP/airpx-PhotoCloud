package at.airpx.model.photo;

import java.time.Instant;

public record PhotoDTO(Long id, String sha256, String filename, Instant capturedAt, long size, String mimeType) {}
