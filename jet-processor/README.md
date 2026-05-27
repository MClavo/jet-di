# Jet Processor

Annotation processor that generates runtime `BeanDefinition` classes consumed by JET.

## What it does

- Processes `@Jet` classes and generates one definition class per type.
- Processes `@Hangar` classes and generates definitions for each enclosed `@Part` method.
- Generates Java source under `<source-package>.generated`.
- Emits `META-INF/services/io.github.mclavo.jet.context.BeanDefinition` in class output so runtime can load definitions through `ServiceLoader`.
- Registers itself as a Java processor via `META-INF/services/javax.annotation.processing.Processor`.

## Main classes

- `JetProcessor`: round coordinator; dispatches `@Jet` and `@Hangar` processing and writes service metadata when processing ends.
- `JetDefinitionFactory`: builds `DefinitionSpec` for class beans and validates constructor injection rules.
- `PartDefinitionFactory`: builds `DefinitionSpec` for `@Part` method beans, including qualifier and primary flags.
- `DefinitionSourceRenderer`: renders final `BeanDefinition` source code (singleton-scoped provider + metadata methods).
- `DefinitionUtils`: shared naming/import/provider-call helpers used by both factories.

## Generation flow

1. `JetProcessor.process(...)` gathers elements for `@Jet` and `@Hangar`.
2. For each `@Hangar`, it first generates a definition for the hangar class itself, then one definition per `@Part` method.
3. Factories produce `DefinitionSpec` values with bean type, qualifier expression, primary flag, and creation expression.
4. `DefinitionSourceRenderer` converts each spec into Java source and `Filer` writes the class.
5. On the final round (`processingOver()`), all generated definition FQCNs are written to ServiceLoader metadata.

## Resolution metadata rules

- `@Fuel` on `@Part` methods sets produced-bean qualifier (`Qualifier.of(...)`); blank/missing value maps to `Qualifier.none()`.
- `@Fuel` on constructor or method parameters produces qualified `beanProvider.provide(...)` calls.
- `@Maverick` marks `@Part` definitions as primary.
- `@Jet` class definitions are always generated with `primary(false)`.
- Generated definitions currently use `ScopeProvider.singletonScope(...)`.

## Validation and diagnostics

- `JetDefinitionFactory` enforces constructor selection: if a class has multiple constructors, exactly one must be annotated with `@Intake`.
- Invalid elements or generation constraints are reported as compiler errors (`DefinitionFactoryException` messages).
- Re-generation conflicts from `Filer` are reported as warnings.
- I/O failures while writing source or metadata are reported as compiler errors.
