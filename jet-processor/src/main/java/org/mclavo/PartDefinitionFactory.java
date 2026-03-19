package org.mclavo;

import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;

import org.mclavo.annotation.Fuel;

public final class PartDefinitionFactory implements SpecDefinitionFactory {

    private static final String METHOD_CREATION_TEMPLATE = "beanProvider.provide(%s.class).%s";
    
    
    @Override
    public DefinitionSpec from(Element element, Elements elements) {
        if (!(element instanceof ExecutableElement methodElement)) {
            throw new IllegalStateException("PartDefinitionFactory only supports methods: " + element.getKind());
        }

        return new DefinitionSpec(
                DefinitionUtils.generatedPackageName(element, elements),
                getClassName(methodElement),
                getBeanType(methodElement),
                DefinitionUtils.qualifierNoneExpression(),
                getCreationExpression(methodElement, elements),
                DefinitionUtils.dependencyImports());
    }

    private String getClassName(ExecutableElement methodElement) {
        String enclosingName = methodElement.getEnclosingElement().getSimpleName().toString();
        String methodName = methodElement.getSimpleName().toString();
        String qualifierSuffix = resolveQualifierSuffix(methodElement);

        return enclosingName + "_" + methodName + "_" + qualifierSuffix + "Definition";
    }

    private String resolveQualifierSuffix(ExecutableElement methodElement) {
        Fuel fuel = methodElement.getAnnotation(Fuel.class);

        if (fuel == null || fuel.value().isBlank()) {
            return "none";
        }

        return sanitizeName(fuel.value());
    }

    private String sanitizeName(String value) {
        return value.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String getBeanType(ExecutableElement methodElement) {
        return methodElement.getReturnType().toString();
    }

    /**
     * Generates the creation expression for the bean by creating a method call to
     * the provided method, and generating provide calls for each of its parameters.
     */
    private String getCreationExpression(ExecutableElement methodElement, Elements elements) {
        String packageName = elements.getPackageOf(methodElement).toString();
        String className = methodElement.getEnclosingElement().getSimpleName().toString();
        String methodCall = createMethodCall(methodElement);

        return METHOD_CREATION_TEMPLATE.formatted(packageName + "." + className, methodCall);
    }

    private String createMethodCall(ExecutableElement methodElement) {
        String methodCallTemplate = methodElement.getSimpleName() + "(%s)";

        // Render the arguments for the method call by creating provide calls for each
        // parameter
        String arguments = methodElement.getParameters().stream()
                .map(DefinitionUtils::provideCall)
                .collect(Collectors.joining(", "));

        return methodCallTemplate.formatted(arguments);
    }
}
