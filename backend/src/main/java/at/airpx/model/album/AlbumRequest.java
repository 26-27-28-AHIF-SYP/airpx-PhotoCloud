package at.airpx.model.album;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumRequest(@NotBlank @Size(max = 100) String name) {}
