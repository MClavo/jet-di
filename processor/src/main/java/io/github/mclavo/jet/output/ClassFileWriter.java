package io.github.mclavo.jet.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

/**
 * Writes a class file to the output directory.
 */
public final class ClassFileWriter {

    private final Filer filer;

    /**
     * Creates a new instance of {@link ClassFileWriter}.
     *
     * @param filer the filer to use for writing class files
     */
    public ClassFileWriter(Filer filer) {
        this.filer = Objects.requireNonNull(filer);
    }

    /**
     * Writes a class file to the output directory.
     *
     * @param className the fully qualified name of the class
     * @param bytecode the bytecode of the class
     * @param origin the element that originated the class file
     * @throws IOException if an I/O error occurs
     */
    public void write(
        String className,
        byte[] bytecode,
        Element origin
    ) throws IOException {

        Objects.requireNonNull(className);
        Objects.requireNonNull(bytecode);
        Objects.requireNonNull(origin);

        JavaFileObject file = filer.createClassFile(
            className,
            topLevelType(origin)
        );

        try (OutputStream output = file.openOutputStream()) {
            output.write(bytecode);
        }
    }

    private Element topLevelType(Element element) {
        Element current = element;

        while (!(current instanceof TypeElement)) {
            current = current.getEnclosingElement();
        }

        while (current.getEnclosingElement() instanceof TypeElement) {
            current = current.getEnclosingElement();
        }

        return current;
    }
}