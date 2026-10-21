package io.github.mclavo.jet.exception;


/**
 * Thrown when multiple bean candidates are found when resolving a dependency, and no unique primary
 * candidate can be determined.
 */
public class MultipleBeanCandidateException extends JetException {

    public MultipleBeanCandidateException(String message) {
        super(message);
    }

}
