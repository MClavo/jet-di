package io.github.mclavo.jet.model;

import java.util.List;
import java.util.Objects;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Represents a strategy to create a bean instance.
 * 
 * <ul>
 *   <li>{@link CreationSpec.Constructor}</li>
 *   <li>{@link CreationSpec.FactoryMethod}</li>
 * </ul>
 */
public sealed interface CreationSpec
    permits CreationSpec.Constructor, CreationSpec.FactoryMethod {

    record Constructor(
        ExecutableElement constructor,
        List<DependencySpec> arguments
    ) implements CreationSpec {

        public Constructor {
            Objects.requireNonNull(constructor);
            arguments = List.copyOf(arguments);
        }
    }

    record FactoryMethod(
        TypeElement owner,
        ExecutableElement method,
        List<DependencySpec> arguments
    ) implements CreationSpec {

        public FactoryMethod {
            Objects.requireNonNull(owner);
            Objects.requireNonNull(method);
            arguments = List.copyOf(arguments);
        }
    }
}