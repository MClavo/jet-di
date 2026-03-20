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

public final class JetDefinitionFactory implements SpecDefinitionFactory {

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
