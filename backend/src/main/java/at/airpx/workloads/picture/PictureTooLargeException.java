package at.airpx.workloads.picture;

public class PictureTooLargeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PictureTooLargeException(String message) {
        super(message);
    }
}
