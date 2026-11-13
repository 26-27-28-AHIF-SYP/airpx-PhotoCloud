package at.airpx.workloads.picture;

import at.airpx.model.photo.PictureDTO;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.util.List;

public interface PictureService {
    List<PictureDTO> list(long userId);
    List<String> missing(long userId, List<String> hashes);
    PictureDTO upload(long userId, FileUpload file, String filename, String capturedAt, String sha256,
                      long declaredSize, String mimeType, String albumId) throws Exception;
}
