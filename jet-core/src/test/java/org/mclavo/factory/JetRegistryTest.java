package org.mclavo.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mclavo.context.BeanDefinition;
import org.mclavo.context.BeanKey;
import org.mclavo.context.BeanProvider;
import org.mclavo.context.Qualifier;
import org.mclavo.exception.DuplicateBeanDefinitionException;

class JetRegistryTest {

    @Test
    void should_register_and_get_singleton_when_class_key_is_used() {
        // given
        JetRegistry registry = new JetRegistry();

        // when
        registry.register(String.class, "value");
        String resolved = registry.get(String.class);

        // then
        assertEquals("value", resolved);
        assertTrue(registry.contains(String.class));
    }

    @Test
    void should_register_and_get_singleton_when_qualified_key_is_used() {
        // given
        JetRegistry registry = new JetRegistry();
        Qualifier qualifier = Qualifier.of("primary");

        // when
        registry.register(String.class, qualifier, "value-primary");
        String resolved = registry.get(String.class, qualifier);

        // then
        assertEquals("value-primary", resolved);
        assertTrue(registry.contains(String.class, qualifier));
    }

    @Test
    void should_return_definition_when_querying_with_matching_qualifier() {
        // given
        JetRegistry registry = new JetRegistry();
        BeanDefinition<String> english = new TestDefinition<>(BeanKey.of(String.class, Qualifier.of("en")), "hello");
        BeanDefinition<String> spanish = new TestDefinition<>(BeanKey.of(String.class, Qualifier.of("es")), "hola");
        registry.register(english);
        registry.register(spanish);

        // when
        BeanDefinition<String> resolved = registry.getDefinition(String.class, Qualifier.of("en"));

        // then
        assertNotNull(resolved);
        assertEquals(Qualifier.of("en"), resolved.qualifier());
    }

    @Test
    void should_throw_duplicate_definition_exception_when_same_key_is_registered_twice() {
        // given
        JetRegistry registry = new JetRegistry();
        BeanKey<String> key = BeanKey.of(String.class, Qualifier.of("same"));
        BeanDefinition<String> first = new TestDefinition<>(key, "first");
        BeanDefinition<String> second = new TestDefinition<>(key, "second");
        registry.register(first);

        // when
        DuplicateBeanDefinitionException exception = assertThrows(
            DuplicateBeanDefinitionException.class,
            () -> registry.register(second));

        // then
        assertTrue(exception.getMessage().contains("Duplicate BeanDefinition"));
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
