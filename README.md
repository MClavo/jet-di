<div align="center">

# Jet DI

**A compact Java dependency injection framework with a flight plan of its own.**

![Java 25](https://img.shields.io/badge/Java-25-orange?style=flat-square)
![Gradle 8.14.3](https://img.shields.io/badge/Gradle-8.14.3-02303A?style=flat-square&logo=gradle&logoColor=white)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mclavo/jet-core?style=flat-square)](https://central.sonatype.com/artifact/io.github.mclavo/jet-core)
[![Build and Test](https://github.com/mclavo/jet-di/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/mclavo/jet-di/actions/workflows/build-and-test.yml)
[![Publish to Maven Central](https://github.com/mclavo/jet-di/actions/workflows/publish-to-maven-central.yml/badge.svg)](https://github.com/mclavo/jet-di/actions/workflows/publish-to-maven-central.yml)
![License](https://img.shields.io/github/license/mclavo/jet-di?style=flat-square)

[Overview](#overview) • [Installation](#installation) • [Getting Started](#getting-started) • [Example Guide](example/README.md) • [Local Development](#local-development) • [Core Concepts](#core-concepts) • [Architecture](#architecture) • [Roadmap](#roadmap) • [Limitations](#limitations)

</div>

## Overview

JET is a small annotation-driven dependency injection framework built in Java. It uses compile-time annotation processing to generate bean definitions, then loads those definitions at runtime with Java's `ServiceLoader`. The result is intentionally simple: no XML, no runtime classpath scanning, and no large framework lifecycle to learn.

JET uses `@Singleton`, `@Inject`, and `@Named` from Jakarta Inject, alongside its own `@Factory`, `@Bean`, and `@Primary` annotations.

> [!NOTE]
> This repository is best read as both a usable mini-framework and an engineering portfolio project. It focuses on dependency resolution, generated metadata, qualifier handling, primary candidate selection, and clear runtime failures rather than breadth of features.

## Quick Example

```java
@Singleton
public class AlertService {

    private final NotificationSender sender;

    @Inject
    public AlertService(@Named("slack") NotificationSender sender) {
        this.sender = sender;
    }

    public void send(String message) {
        sender.send(message);
    }
}
```

```java
@Factory
public class NotificationHangar {

    @Bean
    @Named("slack")
    public NotificationSender slackSender() {
        return new SlackNotificationSender();
    }
}
```

```java
JetContext context = ControlTower.run(App.class);

AlertService service = context.provide(AlertService.class);

service.send("Build completed successfully.");
```

For a complete runnable walkthrough, see the Notification Center example in [`example`](example/README.md).

## Why I Built This

JET was built as a learning-focused project to understand how dependency injection containers work under the hood: annotation processing, generated metadata, service discovery, dependency graphs, scopes, qualifiers, and failure reporting.

It is intentionally small. The goal is not to replace Spring, Guice, or Dagger, but to explore the mechanics behind them with readable code and explicit trade-offs.

## Features

- Compile-time generation of `BeanDefinition` classes for annotated beans and factory methods.
- Runtime container bootstrapped through `ControlTower.run(...)`.
- Lazy singleton creation for generated beans.
- Constructor injection for `@Singleton` classes.
- Factory-method injection through `@Factory` and `@Bean`.
- Type plus qualifier resolution with `@Named` and `Qualifier`.
- Primary candidate selection for `@Bean` methods with `@Primary`.
- Circular dependency detection with a readable dependency path.
- Small exception hierarchy for framework-specific failures.

## Installation

JET is published on Maven Central.

### Gradle

```groovy
dependencies {
    implementation "io.github.mclavo:jet-core:0.5.2"
    annotationProcessor "io.github.mclavo:jet-processor:0.5.2"
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>io.github.mclavo</groupId>
        <artifactId>jet-core</artifactId>
        <version>0.5.2</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.14.0</version>

            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.github.mclavo</groupId>
                        <artifactId>jet-processor</artifactId>
                        <version>0.5.2</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## Project Structure

| Module | Purpose |
| --- | --- |
| `core` | Runtime API, annotations, context, registry, qualifiers, scopes, and exceptions. |
| `processor` | Annotation processor that generates `BeanDefinition` implementations and `META-INF/services` metadata. |
| `example` | Runnable Notification Center sample. See [`example/README.md`](example/README.md) for the walkthrough and experiments. |

## Getting Started

### Prerequisites

- JDK 25
- Gradle is provided through the wrapper (`./gradlew` or `gradlew.bat`)

### Build Everything

```bash
./gradlew build
```

On Windows PowerShell or CMD:

```powershell
.\gradlew.bat build
```

### Run The Example

```bash
./gradlew :example:run
```

On Windows PowerShell or CMD:

```powershell
.\gradlew.bat :example:run
```

The example is the best starting point for learning the framework. Read [`example/README.md`](example/README.md), run the example from the repository root, and then experiment with the annotations in `example/src/main/java/io/github/mclavo/jet/example`.

### Run Core Tests

```bash
./gradlew :core:test
```

## Local Development

Inside this repository, modules use Gradle project dependencies instead of the published Maven Central artifacts:

```groovy
dependencies {
    annotationProcessor project(':processor')
    implementation project(':core')
}
```

`example/build.gradle` also declares `implementation project(':processor')` while developing inside this repository, but application code uses the public runtime API from `core`.

For a complete runnable walkthrough, use the Notification Center guide in [`example/README.md`](example/README.md). It explains `@Singleton`, `@Inject`, `@Factory`, `@Bean`, `@Named`, `@Primary`, and `JetContext.provide(...)` with concrete files you can modify and run.

## Core Concepts

### Annotation Vocabulary

| Annotation | Implemented behavior |
| --- | --- |
| `@Singleton` | Marks a class as a generated container-managed bean, or a factory method as a bean producer. |
| `@Factory` | Marks a class that owns `@Bean` factory methods. A factory with bean methods also gets a generated bean definition so its methods can be called by the container. |
| `@Bean` | Marks a factory method inside a `@Factory`; `@Singleton` can also mark a factory method. The method return type becomes the registered bean type. |
| `@Inject` | Selects the constructor to use when a `@Singleton` or generated factory bean has multiple constructors. |
| `@Named` | Qualifies a `@Bean` method or an injected constructor/factory parameter. Blank values map to no qualifier. |
| `@Primary` | Marks a `@Bean` method as primary for candidate resolution. |
| `@JETMISSION` | Runtime-retained application metadata marker. It is not currently required by `ControlTower.run(...)`. |

`@Altitude`, `@Afterburner`, and `@Ignition` are present as annotations but are not wired into generation or runtime behavior yet.

### Dependency Resolution

JET resolves dependencies by registered type and optional `Qualifier`:

```java
context.provide(MyService.class);
context.provide(NotificationSender.class, "email");
context.provide(MessageFormatter.class, Qualifier.of("json"));
```

Resolution rules are intentionally small:

- If exactly one bean exists for a type, it is returned even if it has a qualifier.
- If multiple beans exist for a type and exactly one is primary, the primary is returned.
- If a qualifier is provided, only candidates with that qualifier are considered.
- If multiple matching candidates remain and there is no unique primary, `MultipleBeanCandidateException` is thrown.
- If no candidate exists, `BeanProvisionException` is thrown.

Generated definitions register the exact type they represent. For `@Singleton` classes, that is the concrete class. For `@Bean` methods, that is the method return type.

### Lifecycle And Scoping

Beans resolved through `JetContext` are lazy singletons. On first resolution, `JetContext.resolve()` calls the generated `BeanDefinition.apply(...)` and stores the instance in its registry's `BeanEntry`. Later requests reuse that instance. Singleton initialization remains synchronized on the entry, and the context's thread-local resolution stack detects circular dependencies.

Generated definitions only construct beans: each direct call to `BeanDefinition.apply(...)` invokes the constructor or `@Bean` factory method again, resolving dependencies through the supplied `BeanProvider`. Definitions do not cache instances; singleton lifecycle management belongs to `JetContext`.

### Injection Patterns

JET intentionally focuses on constructor-based dependency injection.

Implemented injection paths are:
- Constructor parameters on `@Singleton` classes.
- Constructor parameters on generated `@Factory` beans.
- Parameters on `@Bean` factory methods.
- Qualified parameters with `@Named(...)`.

Selected constructors and `@Bean` factory methods must not be private; package-access members are supported. Non-static inner classes cannot be beans because they require an enclosing instance. Declare nested beans `static` or use top-level classes. The processor reports these restrictions as compilation errors before generating their definitions.

> [!NOTE]
> Field and method injection are not supported by design. JET favors constructor injection with `@Inject` because it:
> - makes dependencies explicit at object creation time;
> - improves immutability by allowing `final` fields;
> - makes components easier to test without container magic;
> - avoids partially initialized objects;
> - keeps the dependency graph easier to reason about.



### Configuration Model

JET uses source annotations and generated metadata as configuration. During compilation, `processor` generates one `BeanDefinition` implementation per discovered `@Singleton` class and `@Bean` method. It also writes:

```text
META-INF/services/io.github.mclavo.jet.context.BeanDefinition
```

At runtime, `JetContext` uses `ServiceLoader<BeanDefinition>` to discover and register those definitions. The `bootClass` passed to `ControlTower.run(...)` is currently accepted as bootstrap metadata; it does not filter packages or drive scanning.

### Error Handling

JET reports failures at the phase where they are easiest to understand:

- Compile-time processor errors for invalid generated definitions, such as a class with multiple constructors and no unique `@Inject` constructor.
- `BeanDefinitionLoadingException` when generated definitions cannot be loaded through `ServiceLoader`.
- `BeanProvisionException` when a bean cannot be resolved.
- `MultipleBeanCandidateException` when resolution finds ambiguous candidates.
- `CircularDependencyException` when the active resolution stack detects a dependency cycle.

## Architecture

The framework is split to keep responsibilities narrow:

```mermaid
flowchart LR
    A[Annotated Application Code] --> B[processor]
    B --> C[Generated BeanDefinition classes]
    B --> D[META-INF/services]
    D --> E[ServiceLoader]
    C --> E
    E --> F[JetContext]
    F --> G[Resolved Beans]
```

1. Application code declares beans with annotations from `core`.
2. `processor` runs during compilation and generates simple Java classes implementing `BeanDefinition<T>`.
3. The processor writes ServiceLoader metadata for those generated definitions.
4. `ControlTower.run(...)` creates a `JetContext`.
5. `JetContext` loads definitions, registers them in `JetRegistry`, and resolves beans lazily.
6. Generated definitions call back into `BeanProvider` to resolve constructor and factory-method dependencies.

This design keeps runtime discovery cheap and explicit. The trade-off is that JET depends on annotation processing being configured correctly, and it currently supports a deliberately narrow set of DI features.

## Engineering Trade-Offs

JET favors readable generated code and predictable resolution over feature completeness. A list-backed registry is used instead of a more complex indexing model because the framework is not designed for very large bean graphs. Synchronization around bean creation keeps singleton initialization straightforward. Qualifiers are plain value objects rather than annotation types, which makes them easy to generate and compare.

JET combines Jakarta Inject annotations with factory and primary-candidate annotations defined by JET.

## Roadmap
JET DI is intentionally small, but several areas are good candidates for future versions:

- Evaluate migrating to Java 25 to use Java's Class-File API for direct `.class` generation.
- Prototype-scoped beans through a public scope annotation.
- Stronger annotation processor tests using compile-time testing fixtures.
- Better assignable-type resolution for interfaces and supertypes, while keeping ambiguity rules explicit.
- Primary candidate support for `@Singleton` classes, not only `@Bean` factory methods.

Non-goals remain equally important: JET DI does not aim to become a Spring replacement, a full application framework, or a runtime classpath scanner.

## Limitations

- JET requires Java 25 in the current Gradle configuration.
- `JetContext` supports singleton beans only.
- There are no lifecycle callbacks, shutdown hooks, profiles, conditional beans, or external configuration binding.
- Type resolution uses registered bean types; it does not automatically search assignable implementations for every interface or superclass.
- `@Primary` currently applies to `@Bean` methods, not `@Singleton` classes.
- Duplicate type/qualifier registrations are not rejected at registration time in the current registry implementation; ambiguity is handled during resolution.
- Constructor and factory parameters are always treated as dependencies. Literal configuration values such as strings or primitives are not injected unless a matching bean exists.
