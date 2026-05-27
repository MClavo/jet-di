package io.github.mclavo.jet.exception;

/**
 * Thrown when a source element cannot be converted into a valid bean definition spec.
 */
public class DefinitionFactoryException extends JetException {

    /**
     * @param message error message
     */
    public DefinitionFactoryException(String message) {
        super(message);
    }
}