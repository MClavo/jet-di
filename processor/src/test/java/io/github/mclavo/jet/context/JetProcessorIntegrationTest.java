package io.github.mclavo.jet.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.github.mclavo.jet.JetProcessor;

class JetProcessorIntegrationTest {

    @Test
    void should_generate_class_definitions_and_resolve_real_dependency_graph(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.app.Application", """
            package org.acme.app;

            import io.github.mclavo.jet.annotation.*;

            public class Application {
                public interface Sender {
                    String name();
                }

                public interface Formatter {
                    String format(String message);
                }

                public record SenderValue(String name) implements Sender {
                }

                public record FormatterValue(String prefix) implements Formatter {
                    public String format(String message) {
                        return prefix + "(" + message + ")";
                    }
                }

                @Jet
                public static class AuditService {
                    private final Sender sender;
                    private final Formatter formatter;

                    public AuditService(@Fuel("email") Sender sender, Formatter formatter) {
                        this.sender = sender;
                        this.formatter = formatter;
                    }

                    public String audit(String message) {
                        return sender.name() + ":" + formatter.format(message);
                    }
                }

                @Jet
                public static class NotificationCenter {
                    private final AuditService audit;
                    private final Sender sender;
                    private final Formatter formatter;

                    public NotificationCenter(AuditService audit, Sender sender, @Fuel("json") Formatter formatter) {
                        this.audit = audit;
                        this.sender = sender;
                        this.formatter = formatter;
                    }

                    public String report() {
                        return audit.audit("deployed") + "|default=" + sender.name()
                            + "|json=" + formatter.format("ready");
                    }
                }

                @Hangar
                public static class NotificationHangar {
                    @Part @Fuel("email")
                    public Sender emailSender() {
                        return new SenderValue("email");
                    }

                    @Part @Fuel("slack") @Maverick
                    public Sender slackSender() {
                        return new SenderValue("slack");
                    }

                    @Part @Maverick
                    public Formatter plainFormatter() {
                        return new FormatterValue("plain");
                    }

                    @Part @Fuel("json")
                    public Formatter jsonFormatter() {
                        return new FormatterValue("json");
                    }
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            assertDefinitions(
                compilation, classLoader,
                new DefinitionMetadata("org.acme.app.Application$AuditService", "", false),
                new DefinitionMetadata("org.acme.app.Application$NotificationCenter", "", false),
                new DefinitionMetadata("org.acme.app.Application$NotificationHangar", "", false),
                new DefinitionMetadata("org.acme.app.Application$Sender", "email", false),
                new DefinitionMetadata("org.acme.app.Application$Sender", "slack", true),
                new DefinitionMetadata("org.acme.app.Application$Formatter", "", true),
                new DefinitionMetadata("org.acme.app.Application$Formatter", "json", false)
            );

            JetContext context = new JetContext();
            Class<?> centerType = classLoader.loadClass("org.acme.app.Application$NotificationCenter");
            Object firstCenter = context.provide(centerType);
            Object secondCenter = context.provide(centerType);

            assertSame(firstCenter, secondCenter);
            assertEquals("email:plain(deployed)|default=slack|json=json(ready)", invokeString(firstCenter, "report"));

            Class<?> senderType = classLoader.loadClass("org.acme.app.Application$Sender");
            assertEquals("email", invokeString(context.provide(senderType, "email"), "name"));
            assertEquals("slack", invokeString(context.provide(senderType), "name"));

            // Constructor and instance @Part definitions create on every call;
            // only context resolution caches the resulting singleton.
            for (BeanDefinition<?> definition : ServiceLoader.load(BeanDefinition.class, classLoader)) {
                Object first = definition.apply(context);
                Object second = definition.apply(context);
                Object singleton = context.provide(definition.type(), definition.qualifier());

                assertNotSame(first, second, definition.getClass().getName());
                assertNotSame(first, singleton);
                assertSame(singleton, context.provide(definition.type(), definition.qualifier()));
            }
        });
    }

    @Test
    void should_preserve_qualifiers_with_punctuation_quotes_and_backslashes(@TempDir Path tempDir) throws Exception {
        // given
        String qualifier = "quote \"and\\slash";
        Compilation compilation = compile(tempDir, "org.acme.escape.EscapedHangar", """
            package org.acme.escape;

            import io.github.mclavo.jet.annotation.*;

            @Hangar
            public class EscapedHangar {
                @Part @Fuel("blue-team/v1")
                public String blue() {
                    return "blue";
                }

                @Part @Fuel("quote \\\"and\\\\slash")
                public String weird() {
                    return "weird";
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            assertDefinitions(
                compilation, classLoader,
                new DefinitionMetadata("org.acme.escape.EscapedHangar", "", false),
                new DefinitionMetadata("java.lang.String", "blue-team/v1", false),
                new DefinitionMetadata("java.lang.String", qualifier, false)
            );

            JetContext context = new JetContext();

            assertEquals("blue", context.provide(String.class, "blue-team/v1"));
            assertEquals("weird", context.provide(String.class, qualifier));
        });
    }

    @Test
    void should_treat_blank_factory_and_parameter_qualifiers_as_none(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.blank.BlankHangar", """
            package org.acme.blank;

            import io.github.mclavo.jet.annotation.*;

            @Hangar
            public class BlankHangar {
                @Part @Fuel("   ")
                public String text() {
                    return "ready";
                }

                @Part
                public StringBuilder builder(@Fuel("   ") String text) {
                    return new StringBuilder(text);
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            assertDefinitions(
                compilation, classLoader,
                new DefinitionMetadata("org.acme.blank.BlankHangar", "", false),
                new DefinitionMetadata("java.lang.String", "", false),
                new DefinitionMetadata("java.lang.StringBuilder", "", false)
            );

            JetContext context = new JetContext();

            assertEquals("ready", context.provide(String.class));
            assertEquals("ready", context.provide(StringBuilder.class).toString());
        });
    }

    @Test
    void should_resolve_static_factory_without_instantiating_hangar(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.factory.StaticHangar", """
            package org.acme.factory;

            import io.github.mclavo.jet.annotation.*;

            @Hangar
            public class StaticHangar {
                public StaticHangar() {
                    throw new AssertionError("Static factories do not need a hangar instance");
                }

                @Part
                public static StringBuilder message() {
                    return new StringBuilder("ready");
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            JetContext context = new JetContext();
            BeanDefinition<?> definition = ServiceLoader.load(BeanDefinition.class, classLoader).stream()
                .map(ServiceLoader.Provider::get)
                .filter(candidate -> candidate.type().equals(StringBuilder.class))
                .findFirst()
                .orElseThrow();

            // Static @Part definitions also leave singleton caching to the context.
            Object first = definition.apply(context);
            Object second = definition.apply(context);

            assertEquals(StringBuilder.class, first.getClass());
            assertEquals("ready", first.toString());
            assertNotSame(first, second);
            StringBuilder singleton = context.provide(StringBuilder.class);
            assertEquals("ready", singleton.toString());
            assertNotSame(first, singleton);
            assertSame(singleton, context.provide(StringBuilder.class));
        });
    }

    @Test
    void should_resolve_qualified_parameters_when_factory_methods_are_overloaded(@TempDir Path tempDir)
        throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.factory.OverloadedHangar", """
            package org.acme.factory;

            import io.github.mclavo.jet.annotation.*;

            @Hangar
            public class OverloadedHangar {
                @Part @Fuel("prefix")
                public String message() {
                    return "hello";
                }

                @Part @Fuel("greeting")
                public String message(@Fuel("prefix") String prefix) {
                    return prefix + " world";
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            assertDefinitions(
                compilation, classLoader,
                new DefinitionMetadata("org.acme.factory.OverloadedHangar", "", false),
                new DefinitionMetadata("java.lang.String", "prefix", false),
                new DefinitionMetadata("java.lang.String", "greeting", false)
            );

            JetContext context = new JetContext();

            assertEquals("hello", context.provide(String.class, "prefix"));
            assertEquals("hello world", context.provide(String.class, "greeting"));
        });
    }

    @Test
    void should_distinguish_static_nested_beans_with_same_simple_name(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.nested.Components", """
            package org.acme.nested;

            import io.github.mclavo.jet.annotation.Jet;

            public class Components {
                public static class First {
                    @Jet
                    public static class Engine {
                    }
                }

                public static class Second {
                    @Jet
                    public static class Engine {
                    }
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            assertDefinitions(
                compilation, classLoader,
                new DefinitionMetadata("org.acme.nested.Components$First$Engine", "", false),
                new DefinitionMetadata("org.acme.nested.Components$Second$Engine", "", false)
            );

            JetContext context = new JetContext();
            Class<?> firstType = classLoader.loadClass("org.acme.nested.Components$First$Engine");
            Class<?> secondType = classLoader.loadClass("org.acme.nested.Components$Second$Engine");

            assertEquals(firstType, context.provide(firstType).getClass());
            assertEquals(secondType, context.provide(secondType).getClass());
        });
    }

    @Test
    void should_resolve_record_bean_with_constructor_dependency(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.record.Components", """
            package org.acme.record;

            import io.github.mclavo.jet.annotation.*;

            public class Components {
                @Jet
                public record Message(String text) {
                }

                @Hangar
                public static class Config {
                    @Part
                    public String text() {
                        return "ready";
                    }
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            JetContext context = new JetContext();
            Class<?> messageType = classLoader.loadClass("org.acme.record.Components$Message");

            assertEquals("ready", invokeString(context.provide(messageType), "text"));
        });
    }

    @Test
    void should_resolve_bean_with_package_access_constructor(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.constructor.PackageConstructor", """
            package org.acme.constructor;

            import io.github.mclavo.jet.annotation.Jet;

            @Jet
            public class PackageConstructor {
                PackageConstructor() {
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            JetContext context = new JetContext();
            Class<?> beanType = classLoader.loadClass("org.acme.constructor.PackageConstructor");

            assertEquals(beanType, context.provide(beanType).getClass());
        });
    }

    @Test
    void should_select_intake_constructor_when_multiple_constructors_exist(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.constructor.SelectedConstructor", """
            package org.acme.constructor;

            import io.github.mclavo.jet.annotation.*;

            @Jet
            public class SelectedConstructor {
                private SelectedConstructor(String unused) {
                    throw new AssertionError("The unselected constructor must not be called");
                }

                @Intake
                public SelectedConstructor() {
                }
            }
            """);

        // when
        assertSuccessfulCompilation(compilation);

        // then
        withCompiledClasses(compilation, classLoader -> {
            JetContext context = new JetContext();
            Class<?> beanType = classLoader.loadClass("org.acme.constructor.SelectedConstructor");

            assertEquals(beanType, context.provide(beanType).getClass());
        });
    }

    @Test
    void should_fail_compilation_when_multiple_constructors_have_no_intake(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.NoSelectedConstructor", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.Jet;

            @Jet
            public class NoSelectedConstructor {
                public NoSelectedConstructor() {
                }

                public NoSelectedConstructor(String name) {
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), diagnostics);
        assertTrue(
            diagnostics.contains("has more than one constructor, annotate one with @Intake to inject dependencies"),
            diagnostics
        );
    }

    @Test
    void should_fail_compilation_when_multiple_constructors_have_intake(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.TooManySelectedConstructors", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.*;

            @Jet
            public class TooManySelectedConstructors {
                @Intake
                public TooManySelectedConstructors() {
                }

                @Intake
                public TooManySelectedConstructors(String name) {
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), diagnostics);
        assertTrue(diagnostics.contains("has more than one constructor annotated with @Intake"), diagnostics);
    }

    @Test
    void should_fail_compilation_when_selected_constructor_is_private(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.PrivateConstructor", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.Jet;

            @Jet
            public class PrivateConstructor {
                private PrivateConstructor() {
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), "A private constructor must be rejected before runtime. " + diagnostics);
        assertTrue(diagnostics.contains("PrivateConstructor selected constructor must not be private"), diagnostics);
    }

    @Test
    void should_fail_compilation_when_intake_constructor_is_private(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.PrivateIntakeConstructor", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.*;

            @Jet
            public class PrivateIntakeConstructor {
                public PrivateIntakeConstructor(String unused) {
                }

                @Intake
                private PrivateIntakeConstructor() {
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), diagnostics);
        assertTrue(
            diagnostics.contains("PrivateIntakeConstructor selected constructor must not be private"),
            diagnostics
        );
    }

    @Test
    void should_fail_compilation_when_factory_method_is_private(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.PrivateFactoryHangar", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.*;

            @Hangar
            public class PrivateFactoryHangar {
                @Part
                private String message() {
                    return "unreachable";
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), "A private factory method must be rejected before runtime. " + diagnostics);
        assertTrue(diagnostics.contains("message() factory method annotated with @Part must not be private"), diagnostics);
    }

    @Test
    void should_fail_compilation_when_jet_is_non_static_inner_class(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.Outer", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.Jet;

            public class Outer {
                @Jet
                public class InnerBean {
                    public InnerBean() {
                    }
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), diagnostics);
        assertTrue(
            diagnostics.contains(
                "Outer.InnerBean: non-static inner classes require an enclosing instance and cannot be used as beans; "
                    + "declare the class static or use a top-level class"
            ),
            diagnostics
        );
    }

    @Test
    void should_reject_inner_class_before_selecting_constructor(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(tempDir, "org.acme.invalid.Outer", """
            package org.acme.invalid;

            import io.github.mclavo.jet.annotation.Jet;

            public class Outer {
                @Jet
                public class InnerBean {
                    public InnerBean() {
                    }

                    public InnerBean(String name) {
                    }
                }
            }
            """);

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), diagnostics);
        assertTrue(diagnostics.contains("non-static inner classes require an enclosing instance"), diagnostics);
        assertFalse(diagnostics.contains("annotate one with @Intake"), diagnostics);
        assertFalse(Files.exists(compilation.classes().resolve("org/acme/invalid/Outer$InnerBean$JetDefinition.class")));
    }

    private Compilation compile(Path tempDir, String className, String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run with a JDK, not only a JRE");

        Path classes = Files.createDirectories(tempDir.resolve("classes"));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            List<String> options = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString()
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, options, null, List.of(new TestSource(className, source))
            );
            task.setProcessors(List.of(new JetProcessor()));

            boolean success = Boolean.TRUE.equals(task.call());
            String diagnosticText = diagnostics.getDiagnostics().stream()
                .map(this::formatDiagnostic)
                .collect(Collectors.joining("\n"));

            return new Compilation(success, classes, diagnosticText);
        }
    }

    private void assertSuccessfulCompilation(Compilation compilation) {
        assertTrue(compilation.success(), compilation.diagnostics());
    }

    private void assertDefinitions(
        Compilation compilation, ClassLoader classLoader, DefinitionMetadata... expected
    ) throws Exception {
        Path metadata = compilation.classes().resolve("META-INF/services/io.github.mclavo.jet.context.BeanDefinition");
        List<String> providers = Files.readAllLines(metadata);

        assertEquals(expected.length, providers.size(), "Unexpected number of ServiceLoader providers");
        assertEquals(providers.size(), new HashSet<>(providers).size(), "Duplicate ServiceLoader providers");

        Set<DefinitionMetadata> definitions = new HashSet<>();
        for (String provider : providers) {
            Path classFile = compilation.classes().resolve(provider.replace('.', '/') + ".class");
            assertTrue(Files.isRegularFile(classFile), "Missing class file for " + provider);

            Class<?> definitionType = classLoader.loadClass(provider);
            assertTrue(BeanDefinition.class.isAssignableFrom(definitionType), provider);
            BeanDefinition<?> definition = (BeanDefinition<?>) definitionType.getConstructor().newInstance();

            definitions.add(
                new DefinitionMetadata(
                    definition.type().getName(), definition.qualifier().value(), definition.primary()
                )
            );
        }

        assertEquals(Set.of(expected), definitions);
    }

    private String invokeString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (String) method.invoke(target);
    }

    private void withCompiledClasses(Compilation compilation, ClassLoaderAction action) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader classLoader = new URLClassLoader(
            new URL[] { compilation.classes().toUri().toURL() }, originalClassLoader
        )) {
            Thread.currentThread().setContextClassLoader(classLoader);
            action.run(classLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return diagnostic.getKind() + " " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ROOT);
    }

    private record Compilation(
        boolean success, Path classes, String diagnostics
    ) {}

    private record DefinitionMetadata(
        String type, String qualifier, boolean primary
    ) {}

    private interface ClassLoaderAction {
        void run(ClassLoader classLoader) throws Exception;
    }

    private static final class TestSource extends SimpleJavaFileObject {
        private final String source;

        private TestSource(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
