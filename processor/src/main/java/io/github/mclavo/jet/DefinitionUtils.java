package io.github.mclavo.jet;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import io.github.mclavo.jet.annotation.Fuel;

/**
 * Provides helper methods shared by definition factories during code generation.
 */
final class DefinitionUtils {

    private static final String IMPORT_PACKAGE = "io.github.mclavo.jet.context";
    private static final String CLASS_CREATION_TEMPLATE = "beanProvider.provide(%s.class, %s)";
    private static final List<String> DEPENDENCY_IMPORTS = List.of(
        "BeanDefinition",
        "BeanProvider",
        "ScopeProvider",
        "Qualifier"
    );

    private DefinitionUtils() {}


    /**
     * Resolves the package where generated sources should be created.
     *
     * @param element source element being processed
     * @param elements annotation processing utility facade
     * @return generated package name for the resulting definition source
     */
    static String generatedPackageName(Element element, Elements elements) {
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
    static List<String> dependencyImports() {
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
    static String qualifierNoneExpression() {
        return "Qualifier.none()";
    }

    /**
     * Returns the expression representing a qualified bean key.
     *
     * @param qualifier qualifier string value
     * @return qualifier expression string for a qualified key
     */
    static String qualifierOfExpression(String qualifier) {
        return "Qualifier.of(%s)".formatted(javaStringLiteral(qualifier));
    }

    /**
     * Creates a bean provider call for a method/constructor parameter type.
     *
     * @param element constructor or method parameter
     * @return source expression that resolves the parameter from {@code BeanProvider}
     */
    static String provideCall(VariableElement element) {
        Fuel fuel = element.getAnnotation(Fuel.class);
        return CLASS_CREATION_TEMPLATE.formatted(element.asType(), qualifierExpression(fuel));
    }

    /**
     * Creates comma-separated bean provider calls for all executable parameters.
     *
     * @param executable constructor or method whose parameters are resolved from {@code BeanProvider}
     * @return comma-separated source expressions for executable arguments
     */
    static String provideArguments(ExecutableElement executable) {
        return executable.getParameters().stream()
            .map(DefinitionUtils::provideCall)
            .collect(Collectors.joining(", "));
    }


    // ─────────────────────────── private methods ──────────────────────────

    private static String javaStringLiteral(String value) {
        return '"' + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            + '"';
    }

    private static String qualifierExpression(Fuel fuel) {
        if (fuel == null || fuel.value().isBlank()) {
            return qualifierNoneExpression();
        }

        return qualifierOfExpression(fuel.value());
    }
}
