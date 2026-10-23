package io.github.mclavo.jet.model;

import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * Represents a requested dependency.
 * 
 * @param type the type of the dependency
 * @param qualifier the normalized qualifier of the dependency
 */
public record DependencySpec(
    TypeMirror type,
    String qualifier
) {
    public DependencySpec {
        Objects.requireNonNull(type);
        Objects.requireNonNull(qualifier);
    }
}