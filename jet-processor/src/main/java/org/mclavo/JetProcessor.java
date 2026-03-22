package org.mclavo;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Jet;
import org.mclavo.annotation.Part;
import org.mclavo.exception.DefinitionFactoryException;

/**
 * Annotation processor that generates {@code BeanDefinition} classes for:
 * <ul>
 *   <li>{@code @Jet} classes</li>
 *   <li>{@code @Part} methods declared inside {@code @Hangar} classes</li>
 * </ul>
 * It also generates the ServiceLoader file under {@code META-INF/services}.
 */
@SupportedAnnotationTypes({ 
    "org.mclavo.annotation.Jet",
    "org.mclavo.annotation.Intake",
    "org.mclavo.annotation.Hangar",
    "org.mclavo.annotation.Part",
    "org.mclavo.annotation.Fuel"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JetProcessor extends AbstractProcessor {
    private final Set<String> processedBeanDefinitions = new LinkedHashSet<>();
    private final SpecDefinitionFactory jetDefinitionFactory = new JetDefinitionFactory();
    private final SpecDefinitionFactory partDefinitionFactory = new PartDefinitionFactory();

    /**
     * Main annotation-processing round entrypoint.
     *
     * @param annotations annotation types requested in this round
     * @param roundEnv round environment with annotated elements
     * @return {@code true} to claim supported annotations
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            processJet(roundEnv.getElementsAnnotatedWith(Jet.class));
            processHangar(roundEnv.getElementsAnnotatedWith(Hangar.class));

            generateMetadata();

        } catch (Exception e) {
            processingEnv.getMessager()
                .printMessage(Kind.ERROR, "Exception occurred %s".formatted(e));
        }

        return true;
    }


    /**
     * Generates definitions for all classes annotated with {@code @Jet}.
     *
     * @param annotatedElements elements annotated with {@code @Jet}
     */
    private void processJet(Set<? extends Element> annotatedElements) {
        for (Element element : annotatedElements) {
            generateJetDefinition(element);
        }
    }

    /**
     * Generates definitions for {@code @Hangar} classes and their {@code @Part} methods.
     *
     * @param annotatedElements elements annotated with {@code @Hangar}
     */
    private void processHangar(Set<? extends Element> annotatedElements) {
        for (Element hangarElement : annotatedElements) {
            // @Part methods are discovered from members of each @Hangar class.
            List<? extends Element> partBeans = hangarElement.getEnclosedElements().stream()
                .filter(e -> e.getAnnotation(Part.class) != null)
                .toList();

            if (partBeans.isEmpty()) {
                continue; // No @Part methods, skip hangar definition generation.
            }
            
            // The hangar definition must exist so part definitions can request it via BeanProvider.
            generateJetDefinition(hangarElement);

            // Each @Part method is processed into a separate definition file.
            for (Element element : partBeans) {
                generatePartDefinition(element);
            }
        }
    }


    /**
     * Creates a class definition file for the given element.
     *
     * @param element the element for which to create a definition file
     */
    private void generateJetDefinition(Element element) {
        Elements elements = processingEnv.getElementUtils();
        try {
            DefinitionSpec spec = jetDefinitionFactory.from(element, elements);
            writeDefinitionFile(element, spec);

        } catch (DefinitionFactoryException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                e.getMessage(),
                element
            );
        }

    }

    /**
     * Creates a part definition file for the given element.
     *
     * @param element the element for which to create a definition file
     */
    private void generatePartDefinition(Element element) {
        Elements elements = processingEnv.getElementUtils();
        try {
            DefinitionSpec spec = partDefinitionFactory.from(element, elements);
            writeDefinitionFile(element, spec);

        } catch (DefinitionFactoryException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                e.getMessage(),
                element
            );
        }
    }


    /**
     * Writes a source file for the given definition spec, originating from the provided element.
     *
     * @param element source element that originated the definition
     * @param spec definition metadata to render into a source file
     */
    private void writeDefinitionFile(Element element, DefinitionSpec spec){
        Filer filer = processingEnv.getFiler();
        String source = DefinitionSourceRenderer.render(spec);
        String generatedFqcn = spec.packageName() + "." + spec.simpleClassName();


        try {
            JavaFileObject sourceFile = filer.createSourceFile(generatedFqcn, element);

            try (Writer writer = sourceFile.openWriter()) {
                writer.write(source);
                processedBeanDefinitions.add(generatedFqcn);
            }

        } catch (FilerException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "Could not regenerate " + element + ": " + e.getMessage(),
                element
            );

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Error generating definition for " + element + ": " + e.getMessage(),
                element
            );

        }
    }


    /**
     * Writes the ServiceLoader metadata listing generated definition classes.
     */
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

        } catch (FilerException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.WARNING,
                "Could not regenerate ServiceLoader metadata: " + e.getMessage()
            );

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Error generating ServiceLoader metadata: " + e.getMessage()
            );
        }
    }
}
