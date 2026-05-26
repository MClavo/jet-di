# JET Notification Center Example

This module is the runnable learning example for JET. It shows how the framework wires a small notification center with generated bean definitions, constructor injection, factory methods, qualifiers, and default candidate selection.

The example is deterministic and console-only. It does not use menus, user input, HTTP, databases, or file I/O.

## Run It

From the repository root:

```bash
./gradlew :jet-example:run
```

On Windows PowerShell or CMD:

```powershell
.\gradlew.bat :jet-example:run
```

You should see output for the Notification Center, including generated `BeanDefinition` classes being loaded, a multi-level dependency graph, qualified formatter injection, `@Maverick` default sender selection, and a direct qualified sender lookup.

## Example Shape

```text
jet-example
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

Then read `NotificationCenter.java`. This is a managed `@Jet` class that depends on other beans:

```java
@Jet
public final class NotificationCenter {
    @Intake
    public NotificationCenter(
            AlertService alertService,
            NotificationSender defaultSender,
            @Fuel("json") MessageFormatter jsonFormatter,
            @Fuel("markdown") MessageFormatter markdownFormatter
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
        |   +-- @Fuel("slack") NotificationSender
        |   +-- MessageFormatter defaulted by @Maverick
        +-- NotificationSender defaulted by @Maverick
        +-- @Fuel("json") MessageFormatter
        +-- @Fuel("markdown") MessageFormatter
```

## Annotation Tour

`@Jet` marks a class that JET should manage directly. In this example, `NotificationCenter` and `AlertService` are `@Jet` classes.

`@Intake` selects the constructor JET should use. Constructor parameters are resolved from the active `JetContext`.

`@Hangar` marks a factory/configuration class. In this example, `NotificationHangar` owns the sender and formatter factory methods.

`@Part` marks a factory method. The method return type becomes the registered bean type, so the sender methods return `NotificationSender` and the formatter methods return `MessageFormatter`.

`@Fuel` adds a qualifier. For example, `@Fuel("email")` lets the app request `context.provide(NotificationSender.class, "email")`.

`@Maverick` marks the default candidate when more than one bean exists for the same type. In this example, Slack is the default `NotificationSender`, and plain text is the default `MessageFormatter`.

## Why Factory Methods Return Interfaces

JET resolves by the exact registered bean type. A `@Part` method returning `NotificationSender` is registered under `NotificationSender.class`. A method returning `EmailNotificationSender` would be registered under `EmailNotificationSender.class` instead.

That is why `NotificationHangar` returns interface types for sender and formatter parts:

```java
@Part
@Fuel("email")
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
Move `@Maverick` from `slackSender()` to `consoleSender()` in `NotificationHangar`, then run `./gradlew :jet-example:run` again. The unqualified `NotificationSender` injection should switch to console.

2. Change the default formatter.
Move `@Maverick` from `plainTextFormatter()` to `markdownFormatter()`, then run the example. The unqualified formatter injected into `AlertService` should produce Markdown.

3. Add a new qualified sender.
Create an `SmsNotificationSender`, add a `@Part @Fuel("sms")` method returning `NotificationSender`, then resolve it from `App` with `context.provide(NotificationSender.class, "sms")`.

4. Add a new qualified formatter.
Create a formatter such as `UppercaseFormatter`, add a `@Part @Fuel("uppercase")` method returning `MessageFormatter`, then inject it into `NotificationCenter` with `@Fuel("uppercase") MessageFormatter uppercaseFormatter`.

5. Trigger an ambiguity error on purpose.
Remove `@Maverick` from the sender parts while keeping multiple `NotificationSender` parts, then request `context.provide(NotificationSender.class)`. JET should report that multiple candidates exist.

6. Trigger a missing bean error on purpose.
Ask for a qualifier that does not exist, such as `context.provide(NotificationSender.class, "pager")`. JET should report that it cannot provision that bean.

7. Inspect generated code.
After a build, look under `jet-example/build/generated/sources/annotationProcessor/java/main`. The generated definitions show how JET turns annotations into runtime metadata loaded through `ServiceLoader`.

## Keep In Mind

- Re-run `./gradlew :jet-example:run` after changing annotations so annotation processing regenerates metadata.
- `@Maverick` applies to `@Part` methods, not `@Jet` classes.
- Field injection is not implemented; use constructor injection with `@Intake`.
- Type resolution uses the exact registered return type of a `@Part` method.
