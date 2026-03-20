package org.mclavo.exception;

/**
 * Base unchecked exception for the JET framework.
 */
public class JetException extends RuntimeException {

    public JetException(String message) {
        super(message);
    }

    public JetException(String message, Throwable cause) {
        super(message, cause);
    }

    public JetException(Throwable cause) {
        super(cause);
    }
}