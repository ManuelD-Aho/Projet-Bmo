package akandan.bahou.kassy.commun.util;

public class ExceptionValidation extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public ExceptionValidation(String message) {
        super(message);
    }

    public ExceptionValidation(String message, Throwable cause) {
        super(message, cause);
    }

    public ExceptionValidation(Throwable cause) {
        super(cause);
    }
}