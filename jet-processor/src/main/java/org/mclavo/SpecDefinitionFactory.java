package org.mclavo;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

import org.mclavo.exception.DefinitionFactoryException;

/**
 * Contract for translating annotated program elements into {@link DefinitionSpec}s.
 */
public interface SpecDefinitionFactory {

    /**
     * Creates a generation spec from the given element.
     *
     * @param element annotated element being processed
     * @param elements language-model utility from the processing environment
     * @return definition metadata for source rendering
     * @throws DefinitionFactoryException when the element cannot be translated into a valid definition
     */
    DefinitionSpec from(Element element, Elements elements);
}
