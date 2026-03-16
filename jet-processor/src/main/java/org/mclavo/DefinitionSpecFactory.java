package org.mclavo;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import org.mclavo.annotation.Fuel;
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
    private static String classCreationTemplate = "beanProvider.provide(%s.class)";
    private static String methodCreationTemplate = "beanProvider.provide(%s.class).%s";

    public static DefinitionSpec from(Element element, Elements elements) {

        return new DefinitionSpec(
            getPackageName(element, elements),
            getClassName(element),
            getBeanType(element),
            getQualifier(element),
            getCreationExpression(element, elements),
            getImports()
        );
    }

    private static String getPackageName(Element element, Elements elements) {
        PackageElement packageElement = elements.getPackageOf(element);

        if (packageElement.isUnnamed()) {
            return "generated";
        }

        return packageElement.getQualifiedName() + ".generated";
    }

    private static List<String> getImports() {
        return dependencyImports.stream()
            .sorted()
            .map(dependency -> importPackage + "." + dependency)
            .toList();
    }

    private static String getClassName(Element element) {
        // @Jet on class
        if (element instanceof TypeElement typeElement) {
            return typeElement.getSimpleName() + "Definition";
        }

        // @Part on method
        if (element instanceof ExecutableElement methodElement) {
            String enclosingName = methodElement.getEnclosingElement().getSimpleName().toString();
            String methodName = methodElement.getSimpleName().toString();
            String qualifierSuffix = resolveQualifierSuffix(element);

            return enclosingName + "_" + methodName + "_" + qualifierSuffix + "Definition";
        }

        throw new IllegalStateException("Element type not supported: " + element.getKind());
    }

    private static String resolveQualifierSuffix(Element element) {
        Fuel fuel = element.getAnnotation(Fuel.class);

        if (fuel == null || fuel.value().isBlank()) {
            return "none";
        }

        return sanitizeName(fuel.value());
    }
    private static String sanitizeName(String value) {
        return value.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static String getBeanType(Element element) {
        // delete all "(" and ")", because for @Part methods, 
        // the type will be rendered as "()com.demo.MyType", and we only want "com.demo.MyType"
        
        if(element instanceof TypeElement typeElement) {
            return typeElement.getQualifiedName().toString();//.replaceAll("[()]", "");
        }

        if (element instanceof ExecutableElement methodElement) {
            return methodElement.getReturnType().toString();
        }

        return "";

    }

    // TODO: Implement qualifier logic in @part methods in @Hangar class
    private static String getQualifier(Element element) {
        return "Qualifier.none()";   
    }

    private static String getCreationExpression(Element element, Elements elements) {

        if(element instanceof TypeElement typeElement) {
            return renderConstructorCall(typeElement);
        }

        if(element instanceof ExecutableElement methodElement) {
            String packageName = elements.getPackageOf(element).toString();
            String className = methodElement.getEnclosingElement().getSimpleName().toString();
            String methodCall = createMethodCall(methodElement);

            

            String qualifierSuffix = resolveQualifierSuffix(element);

            return methodCreationTemplate.formatted(packageName + "." + className, methodCall);
        }
        
        return "";
    }

    private static String createMethodCall(ExecutableElement methodElement) {
        String methodCallTemplate = methodElement.getSimpleName() + "(%s)";

        String arguments = methodElement.getParameters().stream()
            .map(DefinitionSpecFactory::renderProvideCall)
            .collect(Collectors.joining(", "));

        return methodCallTemplate.formatted(arguments);
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
        return classCreationTemplate.formatted(element.asType());
    }

}
