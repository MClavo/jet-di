package org.mclavo;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

public final class DefinitionUtils {

    private static final String IMPORT_PACKAGE = "org.mclavo.context";
    private static final List<String> DEPENDENCY_IMPORTS = List.of(
        "BeanDefinition",
        "BeanProvider",
        "BeanKey",
        "ScopeProvider",
        "Qualifier"
    );
    private static final String CLASS_CREATION_TEMPLATE = "beanProvider.provide(%s.class)";

    private DefinitionUtils() {
    }

    public static String generatedPackageName(Element element, Elements elements) {
        PackageElement packageElement = elements.getPackageOf(element);

        if (packageElement.isUnnamed()) {
            return "generated";
        }

        return packageElement.getQualifiedName() + ".generated";
    }

    public static List<String> dependencyImports() {
        return DEPENDENCY_IMPORTS.stream()
            .sorted()
            .map(dependency -> IMPORT_PACKAGE + "." + dependency)
            .toList();
    }

    public static String qualifierNoneExpression() {
        return "Qualifier.none()";
    }

    public static String provideCall(VariableElement element) {
        return CLASS_CREATION_TEMPLATE.formatted(element.asType());
    }
}
