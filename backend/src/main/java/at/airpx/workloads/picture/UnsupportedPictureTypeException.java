package at.airpx.workloads.picture;

public class UnsupportedPictureTypeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UnsupportedPictureTypeException(String message) {
        super(message);
    }
}
