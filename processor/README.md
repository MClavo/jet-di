# Jet Processor

Annotation processor that generates runtime `BeanDefinition` classes consumed by JET.

## What it does

- Processes `@Jet` classes and generates one definition class per type.
- Processes `@Hangar` classes and generates definitions for each enclosed `@Part` method.
- Generates `.class` files in the bean's package using the Java 25 Class-File API.
- Emits `META-INF/services/io.github.mclavo.jet.context.BeanDefinition` in class output so runtime can load definitions through `ServiceLoader`.
- Registers itself as a Java processor via `META-INF/services/javax.annotation.processing.Processor`.

## Main classes

- `JetProcessor`: round coordinator; dispatches `@Jet` and `@Hangar` processing and writes service metadata when processing ends.
- `JetDefinitionAnalyzer`: builds `BeanSpec` for class beans and validates constructor injection rules.
- `PartDefinitionAnalyzer`: builds `BeanSpec` for `@Part` method beans, including qualifier and primary flags.
- `DefinitionBytecodeGenerator`: emits `BeanDefinition` bytecode with metadata methods and direct construction in `apply(...)`.
- `DependencyEmitter`: emits dependency resolution calls through `BeanProvider`.

## Generation flow

1. `JetProcessor.process(...)` gathers elements for `@Jet` and `@Hangar`.
2. For each `@Hangar`, it first generates a definition for the hangar class itself, then one definition per `@Part` method.
3. Analyzers produce `BeanSpec` values with bean type, qualifier, primary flag, and constructor or factory creation metadata.
4. `DefinitionBytecodeGenerator` converts each spec into bytecode and `ClassFileWriter` writes the class through `Filer`. Each `apply(...)` invocation calls the constructor or `@Part` factory directly, resolving dependencies through the supplied `BeanProvider`.
5. On the final round (`processingOver()`), all generated definition FQCNs are written to ServiceLoader metadata.

## Resolution metadata rules

- `@Fuel` on `@Part` methods sets produced-bean qualifier (`Qualifier.of(...)`); blank/missing value maps to `Qualifier.none()`.
- `@Fuel` on constructor or method parameters produces qualified `beanProvider.provide(...)` calls.
- `@Maverick` marks `@Part` definitions as primary.
- `@Jet` class definitions are always generated with `primary(false)`.
- Generated definitions do not cache instances. `JetContext.resolve()` owns singleton lifecycle management, storing instances in `BeanEntry` under its existing entry lock.

## Validation and diagnostics

- `JetDefinitionAnalyzer` rejects non-static inner bean classes before constructor or dependency analysis, with a diagnostic suggesting a static nested or top-level class. Static nested beans remain supported.
- `JetDefinitionAnalyzer` enforces constructor selection: if a class has multiple constructors, exactly one must be annotated with `@Intake`. The selected constructor must not be private; package-access constructors remain supported.
- `PartDefinitionAnalyzer` rejects private `@Part` factory methods before dependency analysis or bytecode generation.
- Invalid elements or generation constraints are reported as compiler errors (`DefinitionFactoryException` messages).
- Re-generation conflicts from `Filer` are reported as warnings.
- I/O failures while writing source or metadata are reported as compiler errors.
