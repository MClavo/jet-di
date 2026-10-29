package io.github.mclavo.jet.analysis;

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import io.github.mclavo.jet.annotation.Primary;
import io.github.mclavo.jet.exception.DefinitionFactoryException;
import io.github.mclavo.jet.model.BeanSpec;
import io.github.mclavo.jet.model.CreationSpec;
import io.github.mclavo.jet.model.DependencySpec;

public final class FactoryDefinitionAnalyzer {

    private final DependencyAnalyzer dependencyAnalyzer;

    public FactoryDefinitionAnalyzer() {
        this.dependencyAnalyzer = new DependencyAnalyzer();
    }

    public BeanSpec analyze(ExecutableElement element) {
        return new BeanSpec(
            element.getReturnType(),
            dependencyAnalyzer.getQualifier(element),
            isPrimary(element),
            analyzeMethod(element),
            element
        );
    }

    private CreationSpec.FactoryMethod analyzeMethod(ExecutableElement methodElement) {
        if (methodElement.getModifiers().contains(Modifier.PRIVATE)) {
            throw new DefinitionFactoryException(
                methodElement + " factory method annotated with @Bean must not be private"
            );
        }

        List<DependencySpec> dependencies = dependencyAnalyzer.analyze(methodElement);

        return new CreationSpec.FactoryMethod(
            (TypeElement) methodElement.getEnclosingElement(),
            methodElement,
            dependencies
        );
    }



    private boolean isPrimary(ExecutableElement methodElement) {
        return methodElement.getAnnotation(Primary.class) != null;
    }
}
