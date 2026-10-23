package io.github.mclavo.jet.analysis;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

import io.github.mclavo.jet.annotation.Fuel;
import io.github.mclavo.jet.model.DependencySpec;

final class DependencyAnalyzer {

    public List<DependencySpec> analyze(ExecutableElement executable) {
        return executable.getParameters().stream()
            .map(this::analyzeParameter)
            .toList();
    }

    private DependencySpec analyzeParameter(VariableElement parameter) {
        return new DependencySpec(parameter.asType(), getQualifier(parameter));
    }

    public String getQualifier(Element element) {
        Fuel fuel = element.getAnnotation(Fuel.class);

        return fuel == null || fuel.value().isBlank()
            ? ""
            : fuel.value();
    }
}
