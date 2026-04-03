package org.mclavo;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Maverick;
import org.mclavo.exception.DefinitionFactoryException;

/**
 * Builds {@link DefinitionSpec}s for {@code @Part} factory methods found in {@code @Hangar} classes.
 */
final class PartDefinitionFactory implements SpecDefinitionFactory {

    private static final String METHOD_CREATION_TEMPLATE = "beanProvider.provide(%s.class).%s";

    
    /**
     * Creates a definition specification from a factory method element.
     *
     * @param element annotated element expected to be a method
     * @param elements annotation processing utility facade
     * @return generation spec describing the part definition source
     * @throws DefinitionFactoryException when the input element is not a method
     */
    @Override
    public DefinitionSpec from(Element element, Elements elements) {
        if (!(element instanceof ExecutableElement methodElement)) {
            throw new DefinitionFactoryException("PartDefinitionFactory only supports methods: " + element.getKind());
        }

        return new DefinitionSpec.Builder()
            .packageName(DefinitionUtils.generatedPackageName(element, elements))
            .imports(DefinitionUtils.dependencyImports())
            .simpleClassName(getClassName(methodElement))
            .beanType(getBeanType(methodElement))
            .qualifierExpression(getQualifierExpression(methodElement))
            .primary(isPrimary(methodElement))
            .creationExpression(getCreationExpression(methodElement, elements))
            .build();
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


    private String getQualifierExpression(ExecutableElement methodElement) {
        Fuel fuel = methodElement.getAnnotation(Fuel.class);
        
        if (fuel == null || fuel.value().isBlank()) {
            return DefinitionUtils.qualifierNoneExpression();
        }

        return DefinitionUtils.qualifierOfExpression(fuel.value());

    }

    private boolean isPrimary(ExecutableElement methodElement) {
        return methodElement.getAnnotation(Maverick.class) != null;
    }

    /**
     * Builds the source expression that invokes the factory method on its enclosing hangar bean.
     *
     * @param methodElement factory method metadata
     * @param elements annotation processing utility facade
     * @return source expression that creates the part bean
     */
    private String getCreationExpression(ExecutableElement methodElement, Elements elements) {
        String packageName = elements.getPackageOf(methodElement).toString();
        String className = methodElement.getEnclosingElement().getSimpleName().toString();
        String methodCall = createMethodCall(methodElement);

        return METHOD_CREATION_TEMPLATE.formatted(packageName + "." + className, methodCall);
    }

    private String createMethodCall(ExecutableElement methodElement) {
        String methodCallTemplate = methodElement.getSimpleName() + "(%s)";

        // Each method parameter is resolved from BeanProvider at runtime.
        String arguments = DefinitionUtils.provideArguments(methodElement);

        return methodCallTemplate.formatted(arguments);
    }
}
