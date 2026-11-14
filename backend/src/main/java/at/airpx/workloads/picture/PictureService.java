package at.airpx.workloads.picture;

import at.airpx.model.picture.PictureDTO;
import java.util.List;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public interface PictureService {
    List<PictureDTO> list(long accountId);

    List<String> missing(long accountId, List<String> hashes);

    PictureDTO upload(
            long accountId,
            FileUpload file,
            String filename,
            String capturedAt,
            String sha256,
            long declaredSize,
            String mimeType,
            String albumId)
            throws Exception;
}
