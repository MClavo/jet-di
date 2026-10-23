package io.github.mclavo.jet.output;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.ServiceLoader;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import io.github.mclavo.jet.context.BeanDefinition;

/**
 * Writes the index that {@link ServiceLoader} uses to find implementations of
 * {@link BeanDefinition}.
 */
public final class ServiceMetadataWriter {

    private static final String SERVICE_FILE = "META-INF/services/io.github.mclavo.jet.context.BeanDefinition";

    private final Filer filer;

    public ServiceMetadataWriter(Filer filer) {
        this.filer = Objects.requireNonNull(filer);
    }

    /**
     * Writes the service metadata file to the output directory.
     *
     * @param definitions the fully qualified names of the bean definitions
     * @throws IOException if an I/O error occurs
     */
    public void write(Collection<String> definitions)
        throws IOException {

        Objects.requireNonNull(definitions);

        if (definitions.isEmpty()) {
            return;
        }

        FileObject file = filer.createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            SERVICE_FILE
        );

        try (var writer = new OutputStreamWriter(
            file.openOutputStream(),
            StandardCharsets.UTF_8
        )) {
            for (String definition : definitions.stream()
                .distinct()
                .sorted()
                .toList()) {

                writer.write(definition);
                writer.write('\n');
            }
        }
    }
}