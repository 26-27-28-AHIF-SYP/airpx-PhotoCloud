package at.airpx.workloads.picture;

import at.airpx.model.picture.PictureDTO;
import java.util.List;

public record PicturePage(List<PictureDTO> pictures, int page, int size, long total) {
    public PicturePage {
        pictures = List.copyOf(pictures);
    }
}
