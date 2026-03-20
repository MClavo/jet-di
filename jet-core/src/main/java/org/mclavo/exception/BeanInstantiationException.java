package org.mclavo.exception;

/**
 * Thrown when bean reflection-based construction fails.
 */
public class BeanInstantiationException extends JetException {

    /**
     * @param message error message
     * @param cause underlying reflection failure
     */
    public BeanInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}