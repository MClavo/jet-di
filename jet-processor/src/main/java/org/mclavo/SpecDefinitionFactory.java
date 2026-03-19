package org.mclavo;

import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;

public interface SpecDefinitionFactory {
    DefinitionSpec from(Element element, Elements elements);
}
