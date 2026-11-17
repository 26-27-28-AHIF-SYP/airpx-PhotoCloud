package at.airpx.workloads.picture;

import java.nio.file.Path;

public record PictureFile(Path path, String mimeType) {}
