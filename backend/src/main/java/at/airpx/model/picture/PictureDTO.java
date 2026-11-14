package at.airpx.model.picture;

import java.time.Instant;

public record PictureDTO(Long id, String sha256, String filename, Instant capturedAt, long size, String mimeType) {}
