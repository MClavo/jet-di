# Jet Core

Core runtime module for JET dependency resolution.

## Responsibilities

- Defines core annotations used by runtime and processor modules.
- Provides context APIs for bean lookup and qualifier handling.
- Stores generated bean definitions and singleton instances in a concurrent registry.
- Instantiates and injects beans at runtime using reflection.
- Exposes a small exception hierarchy for framework-specific failures.

## Main packages

- `org.mclavo.annotation`: bean and factory annotations (`@Jet`, `@Hangar`, `@Part`, `@Intake`, etc.).
- `org.mclavo.context`: bean contracts, qualifier value object, scopes, and runtime context.
- `org.mclavo.factory`: runtime bean factory and registry.
- `org.mclavo.exception`: reusable framework exception types.

## Runtime flow

1. `ControlTower.run(...)` creates a `JetContext`.
2. `JetContext` loads generated `BeanDefinition` implementations through `ServiceLoader`.
3. Bean requests are resolved from `JetRegistry`.
4. If needed, `JetFactory` creates instances and injects `@Intake` fields.
5. Scope behavior is applied through `ScopeProvider` implementations.

## Notes

- Singleton creation uses concurrent/synchronized guards to avoid duplicate initialization.
- Qualifier-aware lookups are currently scaffolded and default to type-only resolution.
