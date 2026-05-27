package io.github.mclavo.jet.exception;

/**
 * Thrown when generated bean definitions cannot be discovered or loaded.
 */
public class BeanDefinitionLoadingException extends JetException {

    /**
     * @param message error message
     * @param cause underlying loading failure
     */
    public BeanDefinitionLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}