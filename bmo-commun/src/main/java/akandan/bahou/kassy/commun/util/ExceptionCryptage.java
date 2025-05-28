package akandan.bahou.kassy.commun.util;

public class ExceptionCryptage extends Exception {

    private static final long serialVersionUID = 1L;

    public ExceptionCryptage(String message) {
        super(message);
    }

    public ExceptionCryptage(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionCryptage(Throwable cause) {
        super(cause);
    }
}