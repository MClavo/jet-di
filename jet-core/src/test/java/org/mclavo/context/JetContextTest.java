package org.mclavo.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mclavo.exception.CircularDependencyException;

class JetContextTest {

    @Test
    void should_resolve_qualified_definition_when_provide_is_called_with_qualifier() throws Exception {
        // given
        JetContext context = new JetContext();
        JetRegistry registry = extractRegistry(context);
        registry.register(new TestDefinition<>("hello", String.class, Qualifier.of("en"), false));
        registry.register(new TestDefinition<>("hola", String.class, Qualifier.of("es"), false));

        // when
        String english = context.provide(String.class, Qualifier.of("en"));
        String spanish = context.provide(String.class, "es");

        // then
        assertEquals("hello", english);
        assertEquals("hola", spanish);
    }

    @Test
    void should_throw_circular_dependency_exception_when_dependency_cycle_exists() throws Exception {
        // given
        JetContext context = new JetContext();
        JetRegistry registry = extractRegistry(context);
        registry.register(new AlphaDefinition());
        registry.register(new BetaDefinition());

        // when
        CircularDependencyException exception = assertThrows(
            CircularDependencyException.class,
            () -> context.provide(Alpha.class));

        // then
        assertTrue(exception.getMessage().contains("Circular dependency detected"));
        assertTrue(exception.getMessage().contains(Alpha.class.getName()));
        assertTrue(exception.getMessage().contains(Beta.class.getName()));
    }

    @Test
    void should_cache_singleton_when_definition_is_resolved_multiple_times() throws Exception {
        // given
        JetContext context = new JetContext();
        JetRegistry registry = extractRegistry(context);
        registry.register(new TestDefinition<>(new SingletonBean(), SingletonBean.class, null, true));

        // when
        SingletonBean first = context.provide(SingletonBean.class);
        SingletonBean second = context.provide(SingletonBean.class);

        // then
        assertSame(first, second);
    }

    private JetRegistry extractRegistry(JetContext context) throws Exception {
        Field registryField = JetContext.class.getDeclaredField("registry");
        registryField.setAccessible(true);
        return (JetRegistry) registryField.get(context);
    }

    private record SingletonBean() {
    }

    private record Alpha(Beta beta) {
    }

    private record Beta(Alpha alpha) {
    }

    private static final class AlphaDefinition implements BeanDefinition<Alpha> {
        @Override
        public Class<Alpha> type() {
            return Alpha.class;
        }

        @Override
        public Qualifier qualifier() {
            return Qualifier.none();
        }

        @Override
        public boolean primary() {
            return false;
        }

        @Override
        public Alpha apply(BeanProvider beanProvider) {
            return new Alpha(beanProvider.provide(Beta.class));
        }
    }

    private static final class BetaDefinition implements BeanDefinition<Beta> {
        @Override
        public Class<Beta> type() {
            return Beta.class;
        }

        @Override
        public Qualifier qualifier() {
            return Qualifier.none();
        }

        @Override
        public boolean primary() {
            return false;
        }

        @Override
        public Beta apply(BeanProvider beanProvider) {
            return new Beta(beanProvider.provide(Alpha.class));
        }
    }

     private static final class TestDefinition<T> implements BeanDefinition<T> {
        private final T value;
        private final Class<T> type;
        private final Qualifier qualifier;
        private final boolean primary;


        private TestDefinition(T value, Class<T> type, Qualifier qualifier, boolean isPrimary) {
            this.value = value;
            this.type = type;
            this.qualifier = qualifier;
            this.primary = isPrimary;
        }

        @Override
        public T apply(BeanProvider beanProvider) {
            return value;
        }

        @Override
        public boolean primary() {
            return primary;
        }

        @Override
        public Qualifier qualifier() {
            return qualifier;
        }

        @Override
        public Class<T> type() {
            return type;
        }
    }
}
