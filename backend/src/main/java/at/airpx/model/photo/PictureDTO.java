package at.airpx.model.photo;

import java.time.Instant;

public record PictureDTO(Long id, String sha256, String filename, Instant capturedAt, long size, String mimeType) {}
