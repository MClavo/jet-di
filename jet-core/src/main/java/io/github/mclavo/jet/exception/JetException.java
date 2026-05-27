package io.github.mclavo.jet.exception;

/**
 * Base unchecked exception for the JET framework.
 */
public class JetException extends RuntimeException {

    /**
     * @param message error message
     */
    public JetException(String message) {
        super(message);
    }

    /**
     * @param message error message
     * @param cause underlying cause
     */
    public JetException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause underlying cause
     */
    public JetException(Throwable cause) {
        super(cause);
    }
}