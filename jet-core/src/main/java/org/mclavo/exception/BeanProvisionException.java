package org.mclavo.exception;

/**
 * Thrown when a requested bean cannot be resolved from the context.
 */
public class BeanProvisionException extends JetException {

    /**
     * @param message error message
     */
    public BeanProvisionException(String message) {
        super(message);
    }
}