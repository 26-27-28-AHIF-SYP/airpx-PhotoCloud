package at.airpx.workloads.picture;

import at.airpx.model.picture.PictureDTO;
import java.util.List;
import java.util.Optional;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public interface PictureService {
    PicturePage list(long accountId, int page, int size);

    List<String> missing(long accountId, List<String> hashes);

    PictureDTO upload(
            long accountId,
            FileUpload file,
            String filename,
            String capturedAt,
            String sha256,
            Long declaredSize,
            String mimeType,
            String albumId);

    Optional<PictureFile> load(long accountId, long pictureId);
}
