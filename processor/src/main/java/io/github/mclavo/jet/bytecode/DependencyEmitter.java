package io.github.mclavo.jet.bytecode;

import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import io.github.mclavo.jet.context.BeanProvider;
import io.github.mclavo.jet.model.DependencySpec;
import static java.lang.constant.ConstantDescs.*;

/**
 * Emits the bytecode to load a {@link DependencySpec} from the provider.
 */
final class DependencyEmitter {

    private static final ClassDesc BEAN_PROVIDER = 
        ClassDesc.of(BeanProvider.class.getName());

    private final TypeDescriptors descriptors;

    DependencyEmitter(TypeDescriptors descriptors) {
        this.descriptors = descriptors;
    }

    /**
     * Emits the bytecode to load a {@link DependencySpec} from the provider.
     * <p>
     * There is no return, the future instance of the dependency will be on the stack after this method is called.
     *
     * @param code The code builder to emit the bytecode to.
     * @param dependency The {@link DependencySpec} to load.
     */
    void emit(CodeBuilder code, DependencySpec dependency) {
        ClassDesc type = descriptors.of(dependency.type());

        if (type.isPrimitive()) {
            throw new IllegalArgumentException(
                "Primitive dependencies are not supported: " + type
            );
        }

        code.aload(1)                        // get provider
            .ldc(type)                      // (argument).class
            .ldc(dependency.qualifier())    // Qualifier
            .invokeinterface(
                BEAN_PROVIDER,
                "provide",
                MethodTypeDesc.of(
                    CD_Object,
                    CD_Class,
                    CD_String
                )
            )
            .checkcast(type);
    }
}
