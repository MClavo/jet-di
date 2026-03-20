package org.mclavo;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * Provides helper methods shared by definition factories during code generation.
 */
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

    /**
     * Resolves the package where generated sources should be created.
        *
        * @param element source element being processed
        * @param elements annotation processing utility facade
        * @return generated package name for the resulting definition source
     */
    public static String generatedPackageName(Element element, Elements elements) {
        PackageElement packageElement = elements.getPackageOf(element);

        if (packageElement.isUnnamed()) {
            return "generated";
        }

        return packageElement.getQualifiedName() + ".generated";
    }

    /**
     * Returns imports required by generated definition classes.
        *
        * @return sorted list of fully qualified imports used in generated sources
     */
    public static List<String> dependencyImports() {
        return DEPENDENCY_IMPORTS.stream()
            .sorted()
            .map(dependency -> IMPORT_PACKAGE + "." + dependency)
            .toList();
    }

    /**
     * Returns the expression representing an unqualified bean key.
     *
     * @return qualifier expression string for an unqualified key
     */
    public static String qualifierNoneExpression() {
        return "Qualifier.none()";
    }

    /**
     * Creates a bean provider call for a method/constructor parameter type.
     *
     * @param element constructor or method parameter
     * @return source expression that resolves the parameter from {@code BeanProvider}
     */
    public static String provideCall(VariableElement element) {
        return CLASS_CREATION_TEMPLATE.formatted(element.asType());
    }
}
