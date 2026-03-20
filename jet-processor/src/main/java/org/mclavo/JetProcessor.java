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

@SupportedAnnotationTypes({ 
    "org.mclavo.annotation.Jet",
    "org.mclavo.annotation.Intake",
    "org.mclavo.annotation.Hangar",
    "org.mclavo.annotation.Part"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class JetProcessor extends AbstractProcessor {
    private final Set<String> processedBeanDefinitions = new LinkedHashSet<>();
    private final SpecDefinitionFactory jetDefinitionFactory = new JetDefinitionFactory();
    private final SpecDefinitionFactory partDefinitionFactory = new PartDefinitionFactory();

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


    private void processJet(Set<? extends Element> annotatedElements) {
        for (Element element : annotatedElements) {
            createDefinitionFile(element, jetDefinitionFactory);
        }
    }

    private void processHangar(Set<? extends Element> annotatedElements) {
        // @Hangar is only supported on classes for now,
        //  but we need to look for @Part methods inside them.
        for(Element hangarElement : annotatedElements) {


            // Now look for @Part methods inside the Hangar and create definitions for them.
            List<? extends Element> partBeans = hangarElement.getEnclosedElements().stream()
                .filter(e -> e.getAnnotation(Part.class) != null)
                .toList();

            // If there are no @Part methods, 
            // skip generating a definition for the Hangar.
            if(partBeans.isEmpty()) {
                return;
            }
            
            // First create the definition for the Hangar itself,
            // so it can be used as a dependency for its parts.
            createDefinitionFile(hangarElement, jetDefinitionFactory);

            for(Element element : partBeans) {
                createDefinitionFile(element, partDefinitionFactory);
            }
        
        }

    }

    private void createDefinitionFile(Element element, SpecDefinitionFactory definitionFactory) {
        Filer filer = processingEnv.getFiler();
        Elements elements = processingEnv.getElementUtils();
        try {

            DefinitionSpec spec = definitionFactory.from(element, elements);
            String source = DefinitionSourceRenderer.render(spec);


            String generatedFqcn = spec.packageName() + "." + spec.simpleClassName();

            JavaFileObject sourceFile = filer.createSourceFile(generatedFqcn, element);

            try (Writer writer = sourceFile.openWriter()) {
                writer.write(source);
                processedBeanDefinitions.add(generatedFqcn);
            }

        } catch (FilerException e) {
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

        } catch (DefinitionFactoryException e) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                e.getMessage(),
                element
            );
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
}
