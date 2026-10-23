package io.github.mclavo.jet.analysis;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import io.github.mclavo.jet.annotation.Intake;
import io.github.mclavo.jet.exception.DefinitionFactoryException;
import io.github.mclavo.jet.model.BeanSpec;
import io.github.mclavo.jet.model.CreationSpec;
import io.github.mclavo.jet.model.DependencySpec;

public final class JetDefinitionAnalyzer {

    private final DependencyAnalyzer dependencyAnalyzer;

    public JetDefinitionAnalyzer() {
        this.dependencyAnalyzer = new DependencyAnalyzer();
    }

    public BeanSpec analyze(TypeElement element) {
        if (element.getNestingKind() == NestingKind.MEMBER
            && !element.getModifiers().contains(Modifier.STATIC)) {
            throw new DefinitionFactoryException(
                element.getQualifiedName()
                    + ": non-static inner classes require an enclosing instance and cannot be used as beans; "
                    + "declare the class static or use a top-level class"
            );
        }

        return new BeanSpec(
            element.asType(),
            "",
            false,
            analyzeConstructor(element),
            element
        );
    }

    /**
     * Builds the source code expression used to create an instance of the target
     * bean.
     * 
     * @param typeElement class metadata from the annotation processing model
     * @return source expression that creates the target bean instance
     */
    private CreationSpec.Constructor analyzeConstructor(TypeElement typeElement) {
        List<ExecutableElement> constructors =
            ElementFilter.constructorsIn(typeElement.getEnclosedElements());

        String beanType = typeElement.getQualifiedName().toString();

        ExecutableElement constructor = constructors.size() == 1
            ? constructors.getFirst()
            : checkForInjectableConstructors(constructors, beanType);

        if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
            throw new DefinitionFactoryException(
                beanType + " selected constructor must not be private"
            );
        }

        List<DependencySpec> arguments = dependencyAnalyzer.analyze(constructor);

        return new CreationSpec.Constructor(constructor, arguments);
    }

    /**
     * Validates and returns the constructor marked with {@code @Intake}.
     *
     * @param constructors constructors declared by the target class
     * @param beanType     fully qualified bean class name used in error messages
     * @return the constructor selected for dependency injection
     * @throws DefinitionFactoryException when zero or multiple constructors are
     *                                        annotated with {@code @Intake}
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
