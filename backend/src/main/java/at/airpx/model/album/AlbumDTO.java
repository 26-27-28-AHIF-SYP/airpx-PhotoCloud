package at.airpx.model.album;

import java.time.LocalDateTime;

public record AlbumDTO (
    Long id,
    String name,
    LocalDateTime createdAt
){}
