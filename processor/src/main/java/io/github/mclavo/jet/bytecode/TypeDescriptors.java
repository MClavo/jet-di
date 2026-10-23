package io.github.mclavo.jet.bytecode;

import java.lang.constant.ClassDesc;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static java.lang.constant.ConstantDescs.*;

/**
 * Converts {@link TypeMirror} to {@link ClassDesc}.
 */
final class TypeDescriptors {

    private final Types types;
    private final Elements elements;

    TypeDescriptors(ProcessingEnvironment env) {
        this.types = env.getTypeUtils();
        this.elements = env.getElementUtils();
    }

    /**
     * Returns the {@link ClassDesc} for the given {@link TypeMirror}.
     *
     * @param type The {@link TypeMirror} to get the descriptor for.
     * @return The {@link ClassDesc} for the given type.
     */
    ClassDesc of(TypeMirror type) {
        TypeMirror erased = types.erasure(type);

        return switch (erased.getKind()) {
            case BOOLEAN -> CD_boolean;
            case BYTE -> CD_byte;
            case SHORT -> CD_short;
            case INT -> CD_int;
            case LONG -> CD_long;
            case CHAR -> CD_char;
            case FLOAT -> CD_float;
            case DOUBLE -> CD_double;
            case VOID -> CD_void;

            case ARRAY -> {
                // Erase the generic type of the array component, e.g. List<String> -> List
                ArrayType array = (ArrayType) erased;
                yield of(array.getComponentType()).arrayType();
            }

            case DECLARED -> {
                DeclaredType declared = (DeclaredType) erased;
                TypeElement element = (TypeElement) declared.asElement();

                // It is important for nested classes, as it could contain $
                String name = elements.getBinaryName(element).toString();

                yield ClassDesc.of(name);
            }

            default -> throw new IllegalArgumentException(
                "Unsupported type: " + type
            );
        };
    }
}