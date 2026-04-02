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
        registry.register(new TestDefinition<>(BeanKey.of(String.class, Qualifier.of("en")), "hello"));
        registry.register(new TestDefinition<>(BeanKey.of(String.class, Qualifier.of("es")), "hola"));

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
        registry.register(new TestDefinition<>(BeanKey.of(SingletonBean.class), new SingletonBean()));

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
        public BeanKey<Alpha> key() {
            return BeanKey.of(Alpha.class);
        }

        @Override
        public Alpha apply(BeanProvider beanProvider) {
            return new Alpha(beanProvider.provide(Beta.class));
        }
    }

    private static final class BetaDefinition implements BeanDefinition<Beta> {
        @Override
        public BeanKey<Beta> key() {
            return BeanKey.of(Beta.class);
        }

        @Override
        public Beta apply(BeanProvider beanProvider) {
            return new Beta(beanProvider.provide(Alpha.class));
        }
    }

    private static final class TestDefinition<T> implements BeanDefinition<T> {
        private final BeanKey<T> key;
        private final T value;

        private TestDefinition(BeanKey<T> key, T value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public BeanKey<T> key() {
            return key;
        }

        @Override
        public T apply(BeanProvider beanProvider) {
            return value;
        }
    }
}
