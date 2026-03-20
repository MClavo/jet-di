# Jet Processor

Simple annotation processor that generates runtime bean definitions used by the JET container.

## What it does

- Scans `@Jet` classes and generates one definition class per bean.
- Scans `@Hangar` classes and generates definitions for each `@Part` method.
- Writes `META-INF/services/org.mclavo.context.BeanDefinition` so generated definitions are loaded by `ServiceLoader`.

## Main classes

- `JetProcessor`: entry point for annotation processing rounds.
- `JetDefinitionFactory`: builds generation specs for class-based beans.
- `PartDefinitionFactory`: builds generation specs for method-based part beans.
- `DefinitionSourceRenderer`: renders Java source from a `DefinitionSpec`.
- `DefinitionUtils`: shared helper methods for package naming, imports, and provider calls.

## Generation flow

1. `JetProcessor.process(...)` receives the current round.
2. For each supported element, it asks a factory for a `DefinitionSpec`.
3. `DefinitionSourceRenderer` turns the spec into Java source text.
4. Source is written through `Filer`.
5. Generated class names are written to the ServiceLoader metadata file.

## Constructor and part rules

- A `@Jet` class with multiple constructors must mark exactly one with `@Intake`.
- `@Part` definitions are only generated for methods declared inside `@Hangar` classes.

## Error handling

- Validation errors from factories are reported as compiler errors.
- File re-generation conflicts are reported as warnings.
- I/O failures during generation are reported as compiler errors.
