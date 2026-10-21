package io.github.mclavo.jet.exception;

/**
 * Thrown when more than one bean definition is registered for the same key.
 */
public class DuplicateBeanDefinitionException extends JetException {

    /**
     * @param message error message with conflicting definition details
     */
    public DuplicateBeanDefinitionException(String message) {
        super(message);
    }
}
