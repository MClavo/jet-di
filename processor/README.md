# Jet Processor

Annotation processor that generates runtime `BeanDefinition` classes consumed by JET.

## What it does

- Processes `@Singleton` classes and generates one definition class per type.
- Processes `@Factory` classes and generates definitions for each enclosed `@Bean` or `@Singleton` method.
- Generates `.class` files in the bean's package using the Java 25 Class-File API.
- Emits `META-INF/services/io.github.mclavo.jet.context.BeanDefinition` in class output so runtime can load definitions through `ServiceLoader`.
- Registers itself as a Java processor via `META-INF/services/javax.annotation.processing.Processor`.

## Main classes

- `JetProcessor`: round coordinator; dispatches `@Singleton` and `@Factory` processing and writes service metadata when processing ends.
- `SingletonDefinitionAnalyzer`: builds `BeanSpec` for class beans and validates constructor injection rules.
- `FactoryDefinitionAnalyzer`: builds `BeanSpec` for `@Bean` or `@Singleton` method beans, including qualifier and primary flags.
- `DefinitionBytecodeGenerator`: emits `BeanDefinition` bytecode with metadata methods and direct construction in `apply(...)`.
- `DependencyEmitter`: emits dependency resolution calls through `BeanProvider`.

## Generation flow

1. `JetProcessor.process(...)` gathers elements for `@Singleton` and `@Factory`.
2. For each `@Factory`, it first generates a definition for the factory class itself, then one definition per `@Bean` or `@Singleton` method.
3. Analyzers produce `BeanSpec` values with bean type, qualifier, primary flag, and constructor or factory creation metadata.
4. `DefinitionBytecodeGenerator` converts each spec into bytecode and `ClassFileWriter` writes the class through `Filer`. Each `apply(...)` invocation calls the constructor or `@Bean` factory directly, resolving dependencies through the supplied `BeanProvider`.
5. On the final round (`processingOver()`), all generated definition FQCNs are written to ServiceLoader metadata.

## Resolution metadata rules

- `@Named` on `@Bean` or `@Singleton` methods sets produced-bean qualifier (`Qualifier.of(...)`); blank/missing value maps to `Qualifier.none()`.
- `@Named` on constructor or method parameters produces qualified `beanProvider.provide(...)` calls.
- `@Primary` marks `@Bean` definitions as primary.
- `@Singleton` class definitions are always generated with `primary(false)`.
- Generated definitions do not cache instances. `JetContext.resolve()` owns singleton lifecycle management, storing instances in `BeanEntry` under its existing entry lock.

## Validation and diagnostics

- `SingletonDefinitionAnalyzer` rejects non-static inner bean classes before constructor or dependency analysis, with a diagnostic suggesting a static nested or top-level class. Static nested beans remain supported.
- `SingletonDefinitionAnalyzer` enforces constructor selection: if a class has multiple constructors, exactly one must be annotated with `@Inject`. The selected constructor must not be private; package-access constructors remain supported.
- `FactoryDefinitionAnalyzer` rejects private `@Bean` or `@Singleton` factory methods before dependency analysis or bytecode generation.
- Invalid elements or generation constraints are reported as compiler errors (`DefinitionFactoryException` messages).
- Re-generation conflicts from `Filer` are reported as warnings.
- I/O failures while writing source or metadata are reported as compiler errors.
