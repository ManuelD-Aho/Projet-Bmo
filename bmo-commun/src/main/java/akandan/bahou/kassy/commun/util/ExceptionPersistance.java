package akandan.bahou.kassy.commun.util;

public class ExceptionPersistance extends Exception {

    private static final long serialVersionUID = 1L;

    public ExceptionPersistance(String message) {
        super(message);
    }

    public ExceptionPersistance(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionPersistance(Throwable cause) {
        super(cause);
    }
}