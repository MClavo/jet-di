package org.mclavo;

import java.util.List;

/**
 * Immutable metadata used to generate one {@code BeanDefinition} class.
 *
 * @param packageName generated class package
 * @param simpleClassName generated class simple name
 * @param beanType fully qualified bean type represented by the definition
 * @param qualifierExpression qualifier expression used by {@code BeanKey}
 * @param creationExpression expression used to create the bean instance
 * @param imports required imports for the generated source
 */
public record DefinitionSpec(
    String packageName,
    String simpleClassName,
    String beanType,
    String qualifierExpression,
    String creationExpression,
    List<String> imports
) {}