# JET Notification Center Example

This module is the runnable learning example for JET. It shows how the framework wires a small notification center with generated bean definitions, constructor injection, factory methods, qualifiers, and default candidate selection.

The example is deterministic and console-only. It does not use menus, user input, HTTP, databases, or file I/O.

## Run It

From the repository root:

```bash
./gradlew :example:run
```

On Windows PowerShell or CMD:

```powershell
.\gradlew.bat :example:run
```

You should see output for the Notification Center, including generated `BeanDefinition` classes being loaded, a multi-level dependency graph, qualified formatter injection, `@Primary` default sender selection, and a direct qualified sender lookup.

## Example Shape

```text
example
+-- src/main/java/org/mclavo/example
    +-- App.java
    +-- notification
        +-- AlertService.java
        +-- NotificationCenter.java
        +-- config
        |   +-- NotificationHangar.java
        +-- formatter
        |   +-- MessageFormatter.java
        |   +-- PlainTextFormatter.java
        |   +-- JsonFormatter.java
        |   +-- MarkdownFormatter.java
        +-- sender
            +-- NotificationSender.java
            +-- EmailNotificationSender.java
            +-- SlackNotificationSender.java
            +-- ConsoleNotificationSender.java
```

## What To Read First

Start with `App.java`. It boots JET with `ControlTower.run(App.class)`, asks the `JetContext` for a `NotificationCenter`, runs the demo, and then performs one direct qualified lookup:

```java
NotificationCenter notificationCenter = context.provide(NotificationCenter.class);
NotificationSender emailSender = context.provide(NotificationSender.class, "email");
```

Then read `NotificationCenter.java`. This is a managed `@Singleton` class that depends on other beans:

```java
@Singleton
public final class NotificationCenter {
    @Inject
    public NotificationCenter(
            AlertService alertService,
            NotificationSender defaultSender,
            @Named("json") MessageFormatter jsonFormatter,
            @Named("markdown") MessageFormatter markdownFormatter
    ) {
        // Fields assigned here.
    }
}
```

This demonstrates a multi-level dependency graph:

```text
App
+-- JetContext
    +-- NotificationCenter
        +-- AlertService
        |   +-- @Named("slack") NotificationSender
        |   +-- MessageFormatter defaulted by @Primary
        +-- NotificationSender defaulted by @Primary
        +-- @Named("json") MessageFormatter
        +-- @Named("markdown") MessageFormatter
```

## Annotation Tour

`@Singleton` marks a class that JET should manage directly. In this example, `NotificationCenter` and `AlertService` are `@Singleton` classes.

`@Inject` selects the constructor JET should use. Constructor parameters are resolved from the active `JetContext`.

`@Factory` marks a factory/configuration class. In this example, `NotificationHangar` owns the sender and formatter factory methods.

`@Bean` or `@Singleton` marks a factory method. The method return type becomes the registered bean type, so the sender methods return `NotificationSender` and the formatter methods return `MessageFormatter`.

`@Named` adds a qualifier. For example, `@Named("email")` lets the app request `context.provide(NotificationSender.class, "email")`.

`@Primary` marks the default candidate when more than one bean exists for the same type. In this example, Slack is the default `NotificationSender`, and plain text is the default `MessageFormatter`.

## Why Factory Methods Return Interfaces

JET resolves by the exact registered bean type. A `@Bean` method returning `NotificationSender` is registered under `NotificationSender.class`. A method returning `EmailNotificationSender` would be registered under `EmailNotificationSender.class` instead.

That is why `NotificationHangar` returns interface types for sender and formatter parts:

```java
@Bean
@Named("email")
public NotificationSender emailSender() {
    return new EmailNotificationSender();
}
```

This keeps these lookups valid:

```java
context.provide(NotificationSender.class, "email");
context.provide(NotificationSender.class);
context.provide(MessageFormatter.class, "json");
```

## Experiments

1. Change the default sender.
Move `@Primary` from `slackSender()` to `consoleSender()` in `NotificationHangar`, then run `./gradlew :example:run` again. The unqualified `NotificationSender` injection should switch to console.

2. Change the default formatter.
Move `@Primary` from `plainTextFormatter()` to `markdownFormatter()`, then run the example. The unqualified formatter injected into `AlertService` should produce Markdown.

3. Add a new qualified sender.
Create an `SmsNotificationSender`, add a `@Bean @Named("sms")` method returning `NotificationSender`, then resolve it from `App` with `context.provide(NotificationSender.class, "sms")`.

4. Add a new qualified formatter.
Create a formatter such as `UppercaseFormatter`, add a `@Bean @Named("uppercase")` method returning `MessageFormatter`, then inject it into `NotificationCenter` with `@Named("uppercase") MessageFormatter uppercaseFormatter`.

5. Trigger an ambiguity error on purpose.
Remove `@Primary` from the sender parts while keeping multiple `NotificationSender` parts, then request `context.provide(NotificationSender.class)`. JET should report that multiple candidates exist.

6. Trigger a missing bean error on purpose.
Ask for a qualifier that does not exist, such as `context.provide(NotificationSender.class, "pager")`. JET should report that it cannot provision that bean.

7. Inspect generated code.
After a build, look under `example/build/generated/sources/annotationProcessor/java/main`. The generated definitions show how JET turns annotations into runtime metadata loaded through `ServiceLoader`.

## Keep In Mind

- Re-run `./gradlew :example:run` after changing annotations so annotation processing regenerates metadata.
- `@Primary` applies to `@Bean` methods, not `@Singleton` classes.
- Field and method injection are not implemented; use constructor injection with `@Inject`.
- Type resolution uses the exact registered return type of a `@Bean` method.
