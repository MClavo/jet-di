package io.github.mclavo.jet.bytecode;

import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

import io.github.mclavo.jet.context.BeanDefinition;
import io.github.mclavo.jet.context.BeanProvider;
import io.github.mclavo.jet.context.Qualifier;
import io.github.mclavo.jet.model.BeanSpec;
import io.github.mclavo.jet.model.CreationSpec;
import io.github.mclavo.jet.model.DependencySpec;

import static java.lang.classfile.ClassFile.*;
import static java.lang.constant.ConstantDescs.*;

public final class DefinitionBytecodeGenerator {

    private static final ClassDesc DEFINITION =
        ClassDesc.of(BeanDefinition.class.getName());

    private static final ClassDesc PROVIDER =
        ClassDesc.of(BeanProvider.class.getName());

    private static final ClassDesc QUALIFIER =
        ClassDesc.of(Qualifier.class.getName());

    private static final MethodTypeDesc CREATE_TYPE =
        MethodTypeDesc.of(CD_Object, PROVIDER);

    private final TypeDescriptors descriptors;
    private final DependencyEmitter dependencies;

    public DefinitionBytecodeGenerator(ProcessingEnvironment env) {
        this.descriptors = new TypeDescriptors(env);
        this.dependencies = new DependencyEmitter(descriptors);
    }

    public byte[] generate(BeanSpec spec, String className) {
        ClassDesc generated = ClassDesc.of(className);
        ClassDesc beanType = descriptors.of(spec.beanType());

        if (!beanType.isClassOrInterface() && !beanType.isArray()) {
            throw new IllegalArgumentException(
                "Unsupported bean type: " + spec.beanType()
            );
        }

        return ClassFile.of().build(generated, cb -> {
            cb.withFlags(ACC_PUBLIC | ACC_FINAL | ACC_SUPER);
            cb.withSuperclass(CD_Object);
            cb.withInterfaceSymbols(DEFINITION);

            emitDefaultConstructor(cb);
            emitType(cb, beanType);
            emitQualifier(cb, spec);
            emitPrimary(cb, spec);
            emitApply(cb, spec);
        });
    }

    private void emitDefaultConstructor(ClassBuilder cb) {
        cb.withMethodBody(
            "<init>",
            MethodTypeDesc.of(CD_void),
            ACC_PUBLIC,
            code -> {
                code.aload(0)
                    .invokespecial(
                        CD_Object,
                        "<init>",
                        MethodTypeDesc.of(CD_void)
                    )
                    .return_();
            }
        );
    }

    private void emitType(ClassBuilder cb, ClassDesc beanType) {
        cb.withMethodBody(
            "type",
            MethodTypeDesc.of(CD_Class),
            ACC_PUBLIC,
            code -> code
                .ldc(beanType)
                .areturn()
        );
    }

    private void emitQualifier(ClassBuilder cb, BeanSpec spec) {
        cb.withMethodBody(
            "qualifier",
            MethodTypeDesc.of(QUALIFIER),
            ACC_PUBLIC,
            code -> {
                if (spec.qualifier().isBlank()) {
                    code.invokestatic(
                        QUALIFIER,
                        "none",
                        MethodTypeDesc.of(QUALIFIER)
                    );
                } else {
                    code.ldc(spec.qualifier())
                        .invokestatic(
                            QUALIFIER,
                            "of",
                            MethodTypeDesc.of(QUALIFIER, CD_String)
                        );
                }

                code.areturn();
            }
        );
    }

    private void emitPrimary(ClassBuilder cb, BeanSpec spec) {
        cb.withMethodBody(
            "primary",
            MethodTypeDesc.of(CD_boolean),
            ACC_PUBLIC,
            code -> {
                if (spec.primary()) {
                    code.iconst_1();
                } else {
                    code.iconst_0();
                }

                code.ireturn();
            }
        );
    }

    private void emitApply(ClassBuilder cb, BeanSpec spec) {
        cb.withMethodBody(
            "apply",
            CREATE_TYPE,
            ACC_PUBLIC,
            code -> {
                switch (spec.creation()) {
                    case CreationSpec.Constructor c ->
                        emitConstructor(code, c);

                    case CreationSpec.FactoryMethod f ->
                        emitFactoryMethod(code, f);
                }

                code.areturn();
            }
        );
    }

    private void emitConstructor(
        CodeBuilder code,
        CreationSpec.Constructor creation
    ) {
        ExecutableElement constructor = creation.constructor();

        ClassDesc owner = descriptors.of(
            constructor.getEnclosingElement().asType()
        );

        code.new_(owner).dup();

        for (DependencySpec dependency : creation.arguments()) {
            dependencies.emit(code, dependency);
        }

        code.invokespecial(
            owner,
            "<init>",
            methodType(constructor, CD_void)
        );
    }

    private void emitFactoryMethod(
        CodeBuilder code,
        CreationSpec.FactoryMethod creation
    ) {
        ExecutableElement method = creation.method();

        ClassDesc owner = descriptors.of(
            creation.owner().asType()
        );

        boolean isStatic =
            method.getModifiers().contains(Modifier.STATIC);

        boolean isInterface =
            creation.owner().getKind().isInterface();

        if (!isStatic) {
            dependencies.emit(
                code,
                new DependencySpec(
                    creation.owner().asType(),
                    ""
                )
            );
        }

        for (DependencySpec dependency : creation.arguments()) {
            dependencies.emit(code, dependency);
        }

        String name = method.getSimpleName().toString();

        MethodTypeDesc signature = methodType(
            method,
            descriptors.of(method.getReturnType())
        );

        if (isStatic) {
            code.invokestatic(owner, name, signature, isInterface);
        } else if (isInterface) {
            code.invokeinterface(owner, name, signature);
        } else {
            code.invokevirtual(owner, name, signature);
        }
    }

    private MethodTypeDesc methodType(
        ExecutableElement method,
        ClassDesc returnType
    ) {
        ClassDesc[] parameters = method.getParameters().stream()
            .map(param -> descriptors.of(param.asType()))
            .toArray(ClassDesc[]::new);

        return MethodTypeDesc.of(returnType, parameters);
    }
}
