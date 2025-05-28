package akandan.bahou.kassy.commun.exceptions;

/**
 * Exception personnalisée ExceptionAuthentification
 */
public class ExceptionAuthentification extends Exception {

    public ExceptionAuthentification(String message) {
        super(message);
    }

    public ExceptionAuthentification(String message, Throwable cause) {
        super(message, cause);
    }
}
