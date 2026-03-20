package org.mclavo;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.mclavo.annotation.Intake;
import org.mclavo.exception.DefinitionFactoryException;

/**
 * Builds {@link DefinitionSpec}s for classes annotated as injectable beans.
 * <p>
 * If a class declares more than one constructor, exactly one must be annotated with {@code @Intake}.
 */
public final class JetDefinitionFactory implements SpecDefinitionFactory {

    /**
     * Creates a definition specification from a class element.
     *
    * @param element annotated element expected to be a class
    * @param elements annotation processing utility facade
    * @return generation spec describing the class definition source
     * @throws DefinitionFactoryException when the input is not a class or constructor rules are invalid
     */
    @Override
    public DefinitionSpec from(Element element, Elements elements) {
        if (!(element instanceof TypeElement typeElement)) {
            throw new DefinitionFactoryException("JetDefinitionFactory only supports classes: " + element.getKind());
        }

        return new DefinitionSpec(
            DefinitionUtils.generatedPackageName(element, elements),
            getClassName(typeElement),
            getBeanType(typeElement),
            DefinitionUtils.qualifierNoneExpression(),
            getCreationExpression(typeElement),
            DefinitionUtils.dependencyImports()
        );
    }

    private String getClassName(TypeElement typeElement) {
        return typeElement.getSimpleName() + "Definition";
    }

    private String getBeanType(TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
    }

    private String getCreationExpression(TypeElement typeElement) {
        return renderConstructorCall(typeElement);
    }

    /**
     * Renders a constructor call with dependency resolution expressions.
     *
     * @param typeElement class metadata from the annotation processing model
     * @return source expression that creates the target bean instance
     */
    private String renderConstructorCall(TypeElement typeElement) {
        List<ExecutableElement> constructors = typeElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(ExecutableElement.class::cast)
            .toList();

        String beanType = typeElement.getQualifiedName().toString();

        if (constructors.isEmpty()) {
            return "new " + beanType + "()";
        }

        ExecutableElement constructor;

        if (constructors.size() > 1) {
            constructor = checkForInjectableConstructors(constructors, beanType);
            
        } else {
            constructor = constructors.get(0);
        }

        String arguments = constructor.getParameters().stream()
            .map(DefinitionUtils::provideCall)
            .collect(Collectors.joining(", "));

        return "new " + beanType + "(" + arguments + ")";
    }

    /**
     * Validates and returns the constructor marked with {@code @Intake}.
        *
        * @param constructors constructors declared by the target class
        * @param beanType fully qualified bean class name used in error messages
        * @return the constructor selected for dependency injection
        * @throws DefinitionFactoryException when zero or multiple constructors are annotated with {@code @Intake}
     */
    private ExecutableElement checkForInjectableConstructors(List<ExecutableElement> constructors, String beanType) {
        List<ExecutableElement> injectConstructors = constructors.stream()
            .filter(e -> e.getAnnotation(Intake.class) != null)
            .toList();

        if (injectConstructors.size() > 1) {
            throw new DefinitionFactoryException(
                beanType + " class has more than one constructor annotated with @Intake : "
                    + injectConstructors
            );
        }

        if (injectConstructors.isEmpty()) {
            throw new DefinitionFactoryException(
                beanType + " class has more than one constructor, annotate one with @Intake to inject dependencies"
            );
        }

        return injectConstructors.get(0);
    }
}
