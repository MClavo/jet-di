package org.mclavo;

import java.util.List;

public record DefinitionSpec(
    String packageName,
    String simpleClassName,
    String beanType,
    String qualifierExpression,
    String creationExpression,
    List<String> imports
) {}