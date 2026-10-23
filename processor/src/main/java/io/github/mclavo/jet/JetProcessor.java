
package io.github.mclavo.jet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;

import io.github.mclavo.jet.analysis.JetDefinitionAnalyzer;
import io.github.mclavo.jet.analysis.PartDefinitionAnalyzer;
import io.github.mclavo.jet.annotation.Hangar;
import io.github.mclavo.jet.annotation.Jet;
import io.github.mclavo.jet.annotation.Part;
import io.github.mclavo.jet.bytecode.DefinitionBytecodeGenerator;
import io.github.mclavo.jet.model.BeanSpec;
import io.github.mclavo.jet.output.ClassFileWriter;
import io.github.mclavo.jet.output.ServiceMetadataWriter;

/**
 * Annotation processor responsible for generating BeanDefinition
 * implementations at compile time.
 *
 * <p>
 * Processes:
 * <ul>
 * <li>Classes annotated with {@code @Jet}</li>
 * <li>Classes annotated with {@code @Hangar} containing {@code @Part}
 * methods</li>
 * </ul>
 *
 * <p>
 * Each bean is analyzed and translated directly into JVM bytecode
 * using the Class-File API. Generated definitions are registered
 * through ServiceLoader metadata.
 */
@SupportedAnnotationTypes({
    "io.github.mclavo.jet.annotation.Jet",
    "io.github.mclavo.jet.annotation.Intake",
    "io.github.mclavo.jet.annotation.Hangar",
    "io.github.mclavo.jet.annotation.Part",
    "io.github.mclavo.jet.annotation.Fuel",
    "io.github.mclavo.jet.annotation.Maverick"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public final class JetProcessor extends AbstractProcessor {

    private JetDefinitionAnalyzer jetAnalyzer;
    private PartDefinitionAnalyzer partAnalyzer;
    private DefinitionBytecodeGenerator generator;
    private ClassFileWriter classFileWriter;
    private ServiceMetadataWriter metadataWriter;

    // Tracks generated class names and their originating elements.
    // Also prevents duplicate generation across processing rounds.
    private final Map<String, Element> generatedDefinitions = new LinkedHashMap<>();

    private boolean generationFailed;
    private boolean metadataWritten;

    /**
     * Initializes the annotation processor and its components.
     *
     * @param env annotation processing environment
     */
    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        jetAnalyzer = new JetDefinitionAnalyzer();
        partAnalyzer = new PartDefinitionAnalyzer();

        generator = new DefinitionBytecodeGenerator(env);
        classFileWriter = new ClassFileWriter(env.getFiler());
        metadataWriter = new ServiceMetadataWriter(env.getFiler());
    }

    /**
     * Main annotation-processing entry point.
     *
     * <p>
     * Bean definitions are generated during regular rounds.
     * ServiceLoader metadata is written once, in the final round.
     *
     * @param annotations annotation types present in this round
     * @param roundEnv    current annotation processing round
     * @return {@code true} to claim the supported annotations
     */
    @Override
    public boolean process(
        Set<? extends TypeElement> annotations,
        RoundEnvironment roundEnv
    ) {
        if (roundEnv.processingOver()) {
            writeMetadata(roundEnv);
            return true;
        }

        if (generationFailed || roundEnv.errorRaised()) {
            return true;
        }

        processJet(roundEnv.getElementsAnnotatedWith(Jet.class));
        processHangar(roundEnv.getElementsAnnotatedWith(Hangar.class));

        return true;
    }

    /**
     * Generates definitions for classes annotated with {@code @Jet}.
     *
     * @param elements annotated elements
     */
    private void processJet(Set<? extends Element> elements) {
        for (Element element : elements) {
            if (!(element instanceof TypeElement type)) {
                reportError("@Jet must annotate a type", element);
                continue;
            }

            if (!isInstantiableType(type)) {
                reportError(
                    "@Jet requires a non-abstract class or record",
                    element
                );
                continue;
            }

            generateJetDefinition(type);
        }
    }

    /**
     * Processes {@code @Hangar} classes and their {@code @Part} methods.
     *
     * <p>
     * The hangar itself must be registered as a bean so that
     * instance factory methods can be invoked through BeanProvider.
     *
     * @param elements annotated hangar elements
     */
    private void processHangar(Set<? extends Element> elements) {
        for (Element element : elements) {
            if (!(element instanceof TypeElement hangar)) {
                reportError("@Hangar must annotate a type", element);
                continue;
            }

            List<ExecutableElement> parts = ElementFilter.methodsIn(hangar.getEnclosedElements())
                .stream()
                .filter(method -> method.getAnnotation(Part.class) != null)
                .toList();

            if (parts.isEmpty()) {
                continue;
            }

            if (!isInstantiableType(hangar)) {
                reportError(
                    "@Hangar requires a non-abstract class or record",
                    hangar
                );
                continue;
            }

            // Register the hangar before processing its factory methods.
            // If it was also annotated with @Jet, generation is skipped.
            generateJetDefinition(hangar);

            for (ExecutableElement part : parts) {
                generatePartDefinition(part);
            }
        }
    }

    /**
     * Analyzes a class and generates its BeanDefinition.
     *
     * @param element bean class
     */
    private void generateJetDefinition(TypeElement element) {
        try {
            BeanSpec spec = jetAnalyzer.analyze(element);

            writeDefinition(
                spec,
                generatedNameForType(element)
            );
        } catch (IOException | RuntimeException e) {
            reportError(
                "Failed to generate definition for "
                    + element.getQualifiedName() + ": " + e,
                element
            );
        }
    }

    /**
     * Analyzes a factory method and generates its BeanDefinition.
     *
     * @param element method annotated with {@code @Part}
     */
    private void generatePartDefinition(ExecutableElement element) {
        try {
            BeanSpec spec = partAnalyzer.analyze(element);

            writeDefinition(
                spec,
                generatedNameForPart(element)
            );
        } catch (IOException | RuntimeException e) {
            reportError(
                "Failed to generate definition for "
                    + element + ": " + e,
                element
            );
        }
    }

    /**
     * Generates and writes a BeanDefinition class.
     *
     * <p>
     * Each binary class name may be generated only once.
     * Repeated processing of the same originating element is ignored,
     * while collisions between different elements are rejected.
     *
     * @param spec      analyzed bean metadata
     * @param className binary name of the generated class
     * @throws IOException if the class file cannot be written
     */
    private void writeDefinition(
        BeanSpec spec,
        String className
    ) throws IOException {

        Element previous = generatedDefinitions.get(className);

        if (previous != null) {
            if (previous.equals(spec.origin())) {
                return;
            }

            throw new IllegalStateException(
                "Generated class name collision: " + className
            );
        }

        byte[] bytecode = generator.generate(spec, className);

        classFileWriter.write(
            className,
            bytecode,
            spec.origin()
        );

        // Register only successfully written definitions.
        generatedDefinitions.put(className, spec.origin());
    }

    /**
     * Writes ServiceLoader metadata after all processing rounds.
     *
     * <p>
     * Metadata is not generated if compilation errors occurred,
     * preventing registration of incomplete definitions.
     *
     * @param roundEnv final processing round
     */
    private void writeMetadata(RoundEnvironment roundEnv) {
        if (metadataWritten
            || generationFailed
            || roundEnv.errorRaised()) {
            return;
        }

        try {
            metadataWriter.write(generatedDefinitions.keySet());
            metadataWritten = true;
        } catch (IOException e) {
            reportError(
                "Failed to generate ServiceLoader metadata: " + e,
                null
            );
        }
    }

    /**
     * Returns the binary name for a generated class definition.
     *
     * <p>
     * Uses the original binary name, supporting nested classes
     * without collisions between identically named inner types.
     *
     * @param type original bean type
     * @return generated class binary name
     */
    private String generatedNameForType(TypeElement type) {
        return processingEnv.getElementUtils()
            .getBinaryName(type) + "$JetDefinition";
    }

    /**
     * Returns a deterministic binary name for a factory method definition.
     *
     * <p>
     * A signature hash distinguishes overloaded methods.
     * Qualifiers and return types are included in the signature.
     *
     * @param method factory method
     * @return generated class binary name
     */
    private String generatedNameForPart(ExecutableElement method) {
        TypeElement owner = (TypeElement) method.getEnclosingElement();

        String ownerName = processingEnv.getElementUtils()
            .getBinaryName(owner).toString();

        String parameters = method.getParameters().stream()
            .map(
                param -> processingEnv.getTypeUtils()
                    .erasure(param.asType()).toString()
            )
            .collect(Collectors.joining(","));

        String signature = method.getSimpleName() + "(" + parameters + ")"
            + processingEnv.getTypeUtils()
                .erasure(method.getReturnType())
            + ":" + method.getAnnotation(
                io.github.mclavo.jet.annotation.Fuel.class
            );

        return ownerName
            + "$Part$"
            + method.getSimpleName()
            + "$"
            + signatureHash(signature)
            + "$JetDefinition";
    }

    /**
     * Creates a stable hexadecimal hash used in generated class names.
     *
     * @param value method signature
     * @return shortened SHA-256 hash
     */
    private String signatureHash(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                "SHA-256 is not available", e
            );
        }
    }

    /**
     * Checks whether a type can be instantiated by the current
     * definition generator.
     *
     * @param type bean or hangar type
     * @return whether the type is a non-abstract class or record
     */
    private boolean isInstantiableType(TypeElement type) {
        return (type.getKind() == ElementKind.CLASS
            || type.getKind() == ElementKind.RECORD)
            && !type.getModifiers().contains(Modifier.ABSTRACT);
    }

    /**
     * Reports a compilation error associated with a source element.
     *
     * @param message diagnostic message
     * @param element originating element, or null for global errors
     */
    private void reportError(String message, Element element) {
        generationFailed = true;

        if (element == null) {
            processingEnv.getMessager().printMessage(
                Kind.ERROR, message
            );
        } else {
            processingEnv.getMessager().printMessage(
                Kind.ERROR, message, element
            );
        }
    }
}
