# Jet Core

Core runtime and public API for JET dependency resolution.

## Responsibilities

- Defines public annotations used by application code and the annotation processor (`@Jet`, `@Hangar`, `@Part`, `@Intake`, `@Fuel`, `@Maverick`).
- Provides runtime resolution contracts (`BeanDefinition`, `BeanProvider`, `Qualifier`).
- Loads generated bean definitions with `ServiceLoader` and keeps them in an in-memory registry.
- Resolves dependencies with qualifier + primary rules and guards against circular dependencies.
- Exposes framework-specific runtime exceptions.

## Main packages

- `io.github.mclavo.jet.annotation`: bean, factory, and qualifier annotations.
- `io.github.mclavo.jet.context`: runtime container, bean keys, and resolution interfaces.
- `io.github.mclavo.jet.exception`: base and specialized unchecked exceptions.

## Runtime flow

1. `ControlTower.run(Class<?>)` creates a `JetContext` (the `bootClass` value is currently not used by runtime discovery).
2. `JetContext` uses `ServiceLoader` to load generated `BeanDefinition` implementations.
3. `JetRegistry` stores definitions keyed by exact bean type and selects candidates by qualifier/primary rules.
4. On `provide(...)`, `JetContext` resolves dependencies recursively and tracks a thread-local resolution stack to detect cycles.
5. `JetContext.resolve()` synchronizes on the selected `BeanEntry`, calls the generated `BeanDefinition.apply(...)` if needed, and caches the singleton in that entry. Subsequent resolutions reuse the cached instance.

## Current behavior notes

- Resolution is based on exact registered type; interface/supertype fallback is not added automatically.
- If no qualifier is requested and only one candidate exists, that candidate is returned even when it has a qualifier.
- `@Intake` supports constructor selection in generated definitions; field injection is not implemented in runtime.
- Generated definitions construct beans on every direct `apply(...)` call; only `JetContext` manages singleton caching and lifecycle.
