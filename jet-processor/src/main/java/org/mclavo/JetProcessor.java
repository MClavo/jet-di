package org.mclavo;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Jet;
import org.mclavo.annotation.Part;

@SupportedAnnotationTypes({ 
    "org.mclavo.annotation.Jet",
    "org.mclavo.annotation.Intake",
    "org.mclavo.annotation.Hangar",
    "org.mclavo.annotation.Part"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JetProcessor extends AbstractProcessor {
    private final Set<String> processedBeanDefinitions = new LinkedHashSet<>();

    private boolean visibleWritten = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            processJet(roundEnv.getElementsAnnotatedWith(Jet.class));
            //processHangar(roundEnv.getElementsAnnotatedWith(Hangar.class));
            //generateProvider();
            generateMetadata();

            /* if(roundEnv.processingOver()) {
            } */

           /*  processIntake(roundEnv.getElementsAnnotatedWith(Intake.class));
            processHangar(roundEnv.getElementsAnnotatedWith(Hangar.class));
            processPart(roundEnv.getElementsAnnotatedWith(Part.class)); */

        } catch (Exception e) {

            processingEnv.getMessager()
                .printMessage(Kind.ERROR, "Exception occurred %s".formatted(e));

        }

        return true;
    }


    private void processJet(Set<? extends Element> annotatedElements) {
        Filer filer = processingEnv.getFiler();
        Elements elements = processingEnv.getElementUtils();

        for (Element element : annotatedElements) {
            try {
                //String source = renderDefinitionSource(element, elements);
                
                DefinitionSpec spec = DefinitionSpecFactory.from(element);
                String source = DefinitionSourceRenderer.render(spec);

                String generatedPackage = resolveGeneratedPackage(element, elements);
                String generatedSimpleName = resolveGeneratedSimpleName(element);
                String generatedFqcn = generatedPackage + "." + generatedSimpleName;

                JavaFileObject sourceFile = filer.createSourceFile(generatedFqcn, element);

                try (Writer writer = sourceFile.openWriter()) {
                    writer.write(source);
                    processedBeanDefinitions.add(generatedFqcn);
                }

            } catch (FilerException e) {
                // Suele pasar en rondas incrementales o si intentas generar dos veces el mismo archivo
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "No se pudo regenerar " + element + ": " + e.getMessage(),
                    element
                );
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Error generando definición para " + element + ": " + e.getMessage(),
                    element
                );
            } catch (IllegalStateException e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    e.getMessage(),
                    element
                );
            }
        }
    }

    private void processHangar(Set<? extends Element> annotatedElements) {
        for(Element hangarElement : annotatedElements) {
            List<? extends Element> partBeans = hangarElement.getEnclosedElements().stream()
                .filter(e -> e.getAnnotation(Part.class) != null)
                .toList();

            Filer filer = processingEnv.getFiler();
            Elements elements = processingEnv.getElementUtils();
            
            for(Element element : partBeans) {
                try {
                //String source = renderDefinitionSource(element, elements);
                
                DefinitionSpec spec = DefinitionSpecFactory.from(element);
                String source = DefinitionSourceRenderer.render(spec);

                String generatedPackage = resolveGeneratedPackage(element, elements);
                String generatedSimpleName = resolveGeneratedSimpleName(element);
                String generatedFqcn = generatedPackage + "." + generatedSimpleName;

                JavaFileObject sourceFile = filer.createSourceFile(generatedFqcn, element);

                try (Writer writer = sourceFile.openWriter()) {
                    writer.write(source);
                    processedBeanDefinitions.add(generatedFqcn);
                }

            } catch (FilerException e) {
                // Suele pasar en rondas incrementales o si intentas generar dos veces el mismo archivo
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "No se pudo regenerar " + element + ": " + e.getMessage(),
                    element
                );
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Error generando definición para " + element + ": " + e.getMessage(),
                    element
                );
            } catch (IllegalStateException e) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    e.getMessage(),
                    element
                );
            }
            }
        
        }

    }


    private void generateMetadata() {
        try {
            FileObject resource = processingEnv.getFiler().createResource(
                StandardLocation.CLASS_OUTPUT,
                "",
                "META-INF/services/org.mclavo.context.BeanDefinition"

            );
            
            try (Writer writer = resource.openWriter()) {
                for(String definition : processedBeanDefinitions) {
                    writer.write(definition);
                    writer.write("\n");
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    private String renderDefinitionSource(Element element, Elements elements) {
        String generatedPackage = resolveGeneratedPackage(element, elements);
        String generatedSimpleName = resolveGeneratedSimpleName(element);

        String beanType = resolveBeanType(element);
        String qualifierExpression = resolveQualifierExpression(element);
        String creationExpression = resolveCreationExpression(element);
        String imports = renderImports(beanType);

        return """
            package %s;

            %s

            public final class %s implements BeanDefinition<%s> {

                private final ScopeProvider<%s> provider =
                    ScopeProvider.singletonScope(beanProvider -> %s);

                @Override
                public %s create(BeanProvider beanProvider) {
                    return provider.apply(beanProvider);
                }

                @Override
                public BeanKey key() {
                    return BeanKey.of(%s.class, %s);
                }
            }
            """.formatted(
            generatedPackage,
            imports,
            generatedSimpleName,
            beanType,
            beanType,
            creationExpression,
            beanType,
            beanType,
            qualifierExpression
        );
    }

    private String renderImports(String beanType) {
    return """
        import org.mclavo.BeanDefinition;
        import org.mclavo.BeanProvider;
        import org.mclavo.BeanKey;
        import org.mclavo.ScopeProvider;
        import org.mclavo.Qualifier;
        """;
}

    private String resolveGeneratedPackage(Element element, Elements elements) {
        PackageElement packageElement = elements.getPackageOf(element);

        if (packageElement.isUnnamed()) {
            return "generated";
        }

        return packageElement.getQualifiedName() + ".generated";
    }

    private String resolveGeneratedSimpleName(Element element) {
        if (element instanceof TypeElement typeElement) {
            return typeElement.getSimpleName() + "Definition";
        }

        if (element instanceof ExecutableElement methodElement) {
            String enclosingName = methodElement.getEnclosingElement().getSimpleName().toString();
            String methodName = methodElement.getSimpleName().toString();
            String qualifierSuffix = resolveQualifierSuffix(element);

            return enclosingName + "_" + methodName + "_" + qualifierSuffix + "Definition";
        }

        throw new IllegalStateException("Tipo de elemento no soportado: " + element.getKind());
    }

    private String resolveBeanType(Element element) {
        if (element instanceof TypeElement typeElement) {
            return typeElement.getQualifiedName().toString();
        }

        if (element instanceof ExecutableElement methodElement) {
            return methodElement.getReturnType().toString();
        }

        throw new IllegalStateException("No se pudo resolver el tipo bean para: " + element);
    }

    private String resolveQualifierExpression(Element element) {
        Fuel fuel = element.getAnnotation(Fuel.class);

        if (fuel == null || fuel.value().isBlank()) {
            return "Qualifier.none()";
        }

        return "Qualifier.of(\"" + fuel.value() + "\")";
    }

    private String resolveQualifierSuffix(Element element) {
        Fuel fuel = element.getAnnotation(Fuel.class);

        if (fuel == null || fuel.value().isBlank()) {
            return "none";
        }

        return sanitizeName(fuel.value());
    }
    private String sanitizeName(String value) {
        return value.replaceAll("[^a-zA-Z0-9_]", "_");
    }


    private String resolveCreationExpression(Element element) {
        if (element instanceof TypeElement typeElement) {
            return renderConstructorCall(typeElement);
        }

        if (element instanceof ExecutableElement methodElement) {
            return renderFactoryMethodCall(methodElement);
        }

        throw new IllegalStateException("No se pudo resolver la creación para: " + element);
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

        if (constructors.size() > 1) {
            throw new IllegalStateException(
                "La clase " + beanType + " tiene varios constructores y esta versión no lo soporta"
            );
        }

        ExecutableElement constructor = constructors.get(0);

        String arguments = constructor.getParameters().stream()
            .map(this::renderProvideCall)
            .collect(Collectors.joining(", "));

        return "new " + beanType + "(" + arguments + ")";
    } 

    private String renderFactoryMethodCall(ExecutableElement methodElement) {
        TypeElement enclosingType = (TypeElement) methodElement.getEnclosingElement();

        String configType = enclosingType.getQualifiedName().toString();
        String methodName = methodElement.getSimpleName().toString();

        String arguments = methodElement.getParameters().stream()
            .map(this::renderProvideCall)
            .collect(Collectors.joining(", "));

        return "beanProvider.provide(" + configType + ".class)." + methodName + "(" + arguments + ")";
    }

    private String renderProvideCall(VariableElement parameter) {
        return "beanProvider.provide(" + parameter.asType() + ".class)";
    }


    private void processIntake(Set<? extends Element> annotatedElements) {
        try {
            FileObject fo = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "intake-processor.txt");
            try (Writer w = fo.openWriter()) {
                w.write("INTAKE PROCESSOR VISUAL OUTPUT\n");
                w.write("Found types: " + annotatedElements + "\n");
            }
            visibleWritten = true;
            
        } catch (FilerException fe) {
            // File already created in an earlier round — not an error for our test
            processingEnv.getMessager().printMessage(Kind.WARNING, "Visible file already exists; skipping write.");
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write visible file: " + e);
        }
    }

    /* private void processHangar(Set<? extends Element> annotatedElements) {
        try {
            FileObject fo = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "hangar-processor.txt");
            try (Writer w = fo.openWriter()) {
                w.write("HANGAR PROCESSOR VISUAL OUTPUT\n");
                w.write("Found types: " + annotatedElements + "\n");
            }
            visibleWritten = true;
            
        } catch (FilerException fe) {
            // File already created in an earlier round — not an error for our test
            processingEnv.getMessager().printMessage(Kind.WARNING, "Visible file already exists; skipping write.");
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write visible file: " + e);
        }
    } */

    private void processPart(Set<? extends Element> annotatedElements) {
        try {
            FileObject fo = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", "part-processor.txt");
            try (Writer w = fo.openWriter()) {
                w.write("PART PROCESSOR VISUAL OUTPUT\n");
                w.write("Found types: " + annotatedElements + "\n");
            }
            visibleWritten = true;
            
        } catch (FilerException fe) {
            // File already created in an earlier round — not an error for our test
            processingEnv.getMessager().printMessage(Kind.WARNING, "Visible file already exists; skipping write.");
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write visible file: " + e);
        }
    }


    private void processBeans(RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(Jet.class);
        Set<TypeElement> types = ElementFilter.typesIn(annotated);



        // Large visible warning so it's easy to notice in terminal
        String banner = "\n" +
            "************************************************************\n" +
            "***  JET PROCESSOR RAN — Found types: " + types + "  ***\n" +
            "************************************************************\n";
        processingEnv.getMessager().printMessage(Kind.WARNING, banner);

        // Also write a visible file into the compilation output (CLASS_OUTPUT)
            try {
                FileObject fo = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", "jet-processor-visible.txt");
                try (Writer w = fo.openWriter()) {
                    w.write("JET PROCESSOR VISUAL OUTPUT\n");
                    w.write("Found types: " + types + "\n");
                    w.write(banner);
                }
                visibleWritten = true;
            } catch (FilerException fe) {
                // File already created in an earlier round — not an error for our test
                processingEnv.getMessager().printMessage(Kind.WARNING, "Visible file already exists; skipping write.");
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Failed to write visible file: " + e);
        }
    }

}
