package at.airpx.workloads.picture;

import at.airpx.model.picture.PictureDTO;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PictureMapper {
    public PictureDTO toDTO(Picture picture) {
        return new PictureDTO(
                picture.getId(),
                picture.getSha256(),
                picture.getFilename(),
                picture.getCapturedAt(),
                picture.getSize(),
                picture.getMimeType());
    }
}
