package org.mclavo;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import org.mclavo.annotation.Intake;

public final class DefinitionSpecFactory {

    private static String importPackage = "org.mclavo.context";
    private static List<String> dependencyImports = List.of(
        "BeanDefinition",
        "BeanProvider",
        "BeanKey",
        "ScopeProvider",
        "Qualifier"  
    );

    // TODO: ADD qualifier
    private static String providerTemplate = "beanProvider.provide(%s.class)";

    public static DefinitionSpec from(Element element) {

        return new DefinitionSpec(
            getPackageName(element),
            getClassName(element),
            getBeanType(element),
            getQualifier(element),
            getCreationExpression(element),
            getImports()
        );
    }

    private static String getPackageName(Element element) {
        int nameLength = element.getSimpleName().length();

        // com.demo.service
        // com.demo / . / simpleName -> service
        // remove simpleName and also the point, therefore also - 1
        int packageLength = element.toString().length() - nameLength - 1;

        return element.toString().substring(0, packageLength);
    }

    private static List<String> getImports() {
        return dependencyImports.stream()
            .sorted()
            .map(dependency -> importPackage + "." + dependency)
            .toList();
    }

    private static String getClassName(Element element) {
        return element.getSimpleName().toString()+"Definition";
    }

    private static String getBeanType(Element element) {
        return element.asType().toString();
    }

    // TODO: Implement qualifier logic in @part methods in @Hangar class
    private static String getQualifier(Element element) {
        return "Qualifier.none()";   
    }

    private static String getCreationExpression(Element element) {

        if(element instanceof TypeElement typeElement) {
            return renderConstructorCall(typeElement);
        }

        // TODO: add factory
        return "";
    }

    private static String renderConstructorCall(TypeElement typeElement) {
        List<ExecutableElement> constructors = typeElement.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(ExecutableElement.class::cast)
            .toList();
        
            String beanType = typeElement.getQualifiedName().toString();

            if(constructors.isEmpty()) {
                return "new " + beanType + "()";
            }

            ExecutableElement constructor;

            if(constructors.size() > 1) {
                constructor = checkForInjectableConstructors(constructors, beanType);
            
            } else {
                constructor = constructors.get(0);
            }

            String arguments = constructor.getParameters().stream()
                .map(DefinitionSpecFactory::renderProvideCall)
                .collect(Collectors.joining(", "));
            
            return "new " + beanType + "(" + arguments + ")";

    }

    private static ExecutableElement checkForInjectableConstructors(List<ExecutableElement> constructors, String beanType) {
        List <ExecutableElement> injectConstructors = constructors.stream()
            .filter(e -> e.getAnnotation(Intake.class) != null)
            .toList();

        if(injectConstructors.size() > 1 ) {
            throw new IllegalStateException(
                beanType + " class has more than one constructor annotated with @Intake : "
                + injectConstructors.toString()
            );
        }

        if(injectConstructors.isEmpty()) {
            throw new IllegalStateException(
                beanType + " class has more than one constructor, annotate one with @Intake to inject dependencies"
            );
        }

        return injectConstructors.get(0);
    }

    private static String renderProvideCall(VariableElement element) {
        // TODO: get qualifier
        //element.getAnnotation(null);
        return providerTemplate.formatted(element.asType());
    }

}
