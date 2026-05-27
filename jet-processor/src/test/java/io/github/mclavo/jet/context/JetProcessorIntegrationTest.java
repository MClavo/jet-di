package io.github.mclavo.jet.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    void should_generate_definitions_and_resolve_real_dependency_graph(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(
            tempDir,
            source("org.acme.app.Sender", """
                package org.acme.app;

                public interface Sender {
                    String name();
                }
                """),
            source("org.acme.app.EmailSender", """
                package org.acme.app;

                public final class EmailSender implements Sender {
                    @Override
                    public String name() {
                        return "email";
                    }
                }
                """),
            source("org.acme.app.SlackSender", """
                package org.acme.app;

                public final class SlackSender implements Sender {
                    @Override
                    public String name() {
                        return "slack";
                    }
                }
                """),
            source("org.acme.app.Formatter", """
                package org.acme.app;

                public interface Formatter {
                    String name();
                    String format(String message);
                }
                """),
            source("org.acme.app.PlainFormatter", """
                package org.acme.app;

                public final class PlainFormatter implements Formatter {
                    @Override
                    public String name() {
                        return "plain";
                    }

                    @Override
                    public String format(String message) {
                        return "plain(" + message + ")";
                    }
                }
                """),
            source("org.acme.app.JsonFormatter", """
                package org.acme.app;

                public final class JsonFormatter implements Formatter {
                    @Override
                    public String name() {
                        return "json";
                    }

                    @Override
                    public String format(String message) {
                        return "json(" + message + ")";
                    }
                }
                """),
            source("org.acme.app.AuditService", """
                package org.acme.app;

                import io.github.mclavo.jet.annotation.Fuel;
                import io.github.mclavo.jet.annotation.Intake;
                import io.github.mclavo.jet.annotation.Jet;

                @Jet
                public final class AuditService {
                    private final Sender sender;
                    private final Formatter formatter;

                    @Intake
                    public AuditService(@Fuel("email") Sender sender, Formatter formatter) {
                        this.sender = sender;
                        this.formatter = formatter;
                    }

                    public String audit(String message) {
                        return sender.name() + ":" + formatter.name() + ":" + formatter.format(message);
                    }
                }
                """),
            source("org.acme.app.NotificationCenter", """
                package org.acme.app;

                import io.github.mclavo.jet.annotation.Fuel;
                import io.github.mclavo.jet.annotation.Intake;
                import io.github.mclavo.jet.annotation.Jet;

                @Jet
                public final class NotificationCenter {
                    private final AuditService auditService;
                    private final Sender defaultSender;
                    private final Formatter jsonFormatter;

                    @Intake
                    public NotificationCenter(
                            AuditService auditService,
                            Sender defaultSender,
                            @Fuel("json") Formatter jsonFormatter
                    ) {
                        this.auditService = auditService;
                        this.defaultSender = defaultSender;
                        this.jsonFormatter = jsonFormatter;
                    }

                    public String report() {
                        return auditService.audit("deployed")
                                + "|default=" + defaultSender.name()
                                + "|json=" + jsonFormatter.format("ready");
                    }
                }
                """),
            source("org.acme.app.NotificationHangar", """
                package org.acme.app;

                import io.github.mclavo.jet.annotation.Fuel;
                import io.github.mclavo.jet.annotation.Hangar;
                import io.github.mclavo.jet.annotation.Maverick;
                import io.github.mclavo.jet.annotation.Part;

                @Hangar
                public class NotificationHangar {
                    @Part
                    @Fuel("email")
                    public Sender emailSender() {
                        return new EmailSender();
                    }

                    @Part
                    @Fuel("slack")
                    @Maverick
                    public Sender slackSender() {
                        return new SlackSender();
                    }

                    @Part
                    @Maverick
                    public Formatter plainFormatter() {
                        return new PlainFormatter();
                    }

                    @Part
                    @Fuel("json")
                    public Formatter jsonFormatter() {
                        return new JsonFormatter();
                    }
                }
                """)
        );

        // when
        assertSuccessfulCompilation(compilation);

        // then
        assertGeneratedSource(compilation, "org/acme/app/generated/NotificationCenterDefinition.java");
        assertGeneratedSource(compilation, "org/acme/app/generated/AuditServiceDefinition.java");
        assertGeneratedSource(compilation, "org/acme/app/generated/NotificationHangarDefinition.java");
        assertGeneratedSource(compilation, "org/acme/app/generated/NotificationHangar_emailSender_emailDefinition.java");
        assertGeneratedSource(compilation, "org/acme/app/generated/NotificationHangar_slackSender_slackDefinition.java");
        assertGeneratedSource(compilation, "org/acme/app/generated/NotificationHangar_plainFormatter_noneDefinition.java");
        assertGeneratedSource(compilation, "org/acme/app/generated/NotificationHangar_jsonFormatter_jsonDefinition.java");

        String metadata = serviceMetadata(compilation);
        assertTrue(metadata.contains("org.acme.app.generated.NotificationCenterDefinition"));
        assertTrue(metadata.contains("org.acme.app.generated.AuditServiceDefinition"));
        assertTrue(metadata.contains("org.acme.app.generated.NotificationHangarDefinition"));
        assertTrue(metadata.contains("org.acme.app.generated.NotificationHangar_emailSender_emailDefinition"));
        assertTrue(metadata.contains("org.acme.app.generated.NotificationHangar_slackSender_slackDefinition"));
        assertTrue(metadata.contains("org.acme.app.generated.NotificationHangar_plainFormatter_noneDefinition"));
        assertTrue(metadata.contains("org.acme.app.generated.NotificationHangar_jsonFormatter_jsonDefinition"));

        withCompiledClasses(compilation, classLoader -> {
            JetContext context = new JetContext();
            Class<?> centerType = classLoader.loadClass("org.acme.app.NotificationCenter");
            Object firstCenter = provide(context, centerType);
            Object secondCenter = provide(context, centerType);

            assertSame(firstCenter, secondCenter);
            assertEquals(
                "email:plain:plain(deployed)|default=slack|json=json(ready)",
                invokeString(firstCenter, "report")
            );

            Class<?> senderType = classLoader.loadClass("org.acme.app.Sender");
            assertEquals("email", invokeString(provide(context, senderType, "email"), "name"));
            assertEquals("slack", invokeString(provide(context, senderType), "name"));
            return null;
        });
    }

    @Test
    void should_generate_valid_code_when_qualifier_requires_escaping_and_name_sanitizing(@TempDir Path tempDir)
            throws Exception {
        // given
        String qualifier = "quote \"and\\slash";
        Compilation compilation = compile(
            tempDir,
            source("org.acme.escape.Channel", """
                package org.acme.escape;

                public interface Channel {
                    String id();
                }
                """),
            source("org.acme.escape.ChannelImpl", """
                package org.acme.escape;

                public final class ChannelImpl implements Channel {
                    private final String id;

                    public ChannelImpl(String id) {
                        this.id = id;
                    }

                    @Override
                    public String id() {
                        return id;
                    }
                }
                """),
            source("org.acme.escape.EscapedHangar", """
                package org.acme.escape;

                import io.github.mclavo.jet.annotation.Fuel;
                import io.github.mclavo.jet.annotation.Hangar;
                import io.github.mclavo.jet.annotation.Part;

                @Hangar
                public class EscapedHangar {
                    @Part
                    @Fuel("blue-team/v1")
                    public Channel blue() {
                        return new ChannelImpl("blue");
                    }

                    @Part
                    @Fuel("quote \\\"and\\\\slash")
                    public Channel weird() {
                        return new ChannelImpl("weird");
                    }
                }
                """)
        );

        // when
        assertSuccessfulCompilation(compilation);

        // then
        assertGeneratedSource(compilation, "org/acme/escape/generated/EscapedHangar_blue_blue_team_v1Definition.java");
        assertTrue(
            Files.readString(generatedSource(compilation, "org/acme/escape/generated/EscapedHangar_weird_quote__and_slashDefinition.java"))
                .contains("Qualifier.of(\"quote \\\"and\\\\slash\")")
        );

        withCompiledClasses(compilation, classLoader -> {
            JetContext context = new JetContext();
            Class<?> channelType = classLoader.loadClass("org.acme.escape.Channel");
            Object channel = provide(context, channelType, qualifier);

            assertEquals("weird", invokeString(channel, "id"));
            return null;
        });
    }

    @Test
    void should_fail_compilation_when_constructor_injection_is_ambiguous(@TempDir Path tempDir) throws Exception {
        // given
        Compilation compilation = compile(
            tempDir,
            source("org.acme.invalid.InvalidConstructors", """
                package org.acme.invalid;

                import io.github.mclavo.jet.annotation.Intake;
                import io.github.mclavo.jet.annotation.Jet;

                public final class InvalidConstructors {
                    @Jet
                    public static final class NoSelectedConstructor {
                        public NoSelectedConstructor() {
                        }

                        public NoSelectedConstructor(String name) {
                        }
                    }

                    @Jet
                    public static final class TooManySelectedConstructors {
                        @Intake
                        public TooManySelectedConstructors() {
                        }

                        @Intake
                        public TooManySelectedConstructors(String name) {
                        }
                    }
                }
                """)
        );

        // when
        String diagnostics = compilation.diagnostics();

        // then
        assertFalse(compilation.success(), diagnostics);
        assertTrue(
            diagnostics.contains("has more than one constructor, annotate one with @Intake to inject dependencies"),
            diagnostics
        );
        assertTrue(
            diagnostics.contains("has more than one constructor annotated with @Intake"),
            diagnostics
        );
    }

    private Compilation compile(Path tempDir, TestSource... sources) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run with a JDK, not only a JRE");

        Path classes = Files.createDirectories(tempDir.resolve("classes"));
        Path generatedSources = Files.createDirectories(tempDir.resolve("generated-sources"));
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null)) {
            List<String> options = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-d", classes.toString(),
                "-s", generatedSources.toString()
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                Arrays.asList(sources)
            );
            task.setProcessors(List.of(new JetProcessor()));

            boolean success = Boolean.TRUE.equals(task.call());
            String diagnosticText = diagnostics.getDiagnostics().stream()
                .map(this::formatDiagnostic)
                .collect(Collectors.joining("\n"));

            return new Compilation(success, classes, generatedSources, diagnosticText);
        }
    }

    private TestSource source(String className, String source) {
        return new TestSource(className, source);
    }

    private void assertSuccessfulCompilation(Compilation compilation) {
        assertTrue(compilation.success(), compilation.diagnostics());
    }

    private void assertGeneratedSource(Compilation compilation, String relativePath) {
        assertTrue(Files.exists(generatedSource(compilation, relativePath)), relativePath);
    }

    private Path generatedSource(Compilation compilation, String relativePath) {
        return compilation.generatedSources().resolve(relativePath);
    }

    private String serviceMetadata(Compilation compilation) throws IOException {
        return Files.readString(
            compilation.classes().resolve("META-INF/services/io.github.mclavo.jet.context.BeanDefinition")
        );
    }

    private Object provide(JetContext context, Class<?> type) {
        return context.provide(type);
    }

    private Object provide(JetContext context, Class<?> type, String qualifier) {
        return context.provide(type, qualifier);
    }

    private String invokeString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return (String) method.invoke(target);
    }

    private <T> T withCompiledClasses(Compilation compilation, ClassLoaderAction<T> action) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader classLoader = new URLClassLoader(
            new URL[] { compilation.classes().toUri().toURL() },
            originalClassLoader
        )) {
            Thread.currentThread().setContextClassLoader(classLoader);
            return action.run(classLoader);

        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return diagnostic.getKind() + " " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(Locale.ROOT);
    }

    private record Compilation(boolean success, Path classes, Path generatedSources, String diagnostics) {
    }

    private interface ClassLoaderAction<T> {
        T run(ClassLoader classLoader) throws Exception;
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
