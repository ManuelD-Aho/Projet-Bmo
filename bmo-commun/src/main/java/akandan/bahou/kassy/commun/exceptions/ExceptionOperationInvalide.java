package akandan.bahou.kassy.commun.exceptions;

/**
 * Exception personnalisée ExceptionOperationInvalide
 */
public class ExceptionOperationInvalide extends Exception {

    public ExceptionOperationInvalide(String message) {
        super(message);
    }

    public ExceptionOperationInvalide(String message, Throwable cause) {
        super(message, cause);
    }
}
