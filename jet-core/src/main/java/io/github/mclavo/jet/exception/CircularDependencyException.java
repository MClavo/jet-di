package io.github.mclavo.jet.exception;

/**
 * Thrown when bean resolution detects a dependency cycle.
 */
public class CircularDependencyException extends BeanProvisionException {

    /**
     * @param message error message with dependency cycle details
     */
    public CircularDependencyException(String message) {
        super(message);
    }
}
