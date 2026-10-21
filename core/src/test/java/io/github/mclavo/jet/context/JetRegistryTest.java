package io.github.mclavo.jet.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import io.github.mclavo.jet.exception.MultipleBeanCandidateException;

class JetRegistryTest {

    @Test
    void should_register_and_resolve_single_definition_when_no_qualifier_is_used() {
        // given
        JetRegistry registry = new JetRegistry();
        BeanDefinition<String> definition = new TestDefinition<>("value", String.class, Qualifier.none(), false);
        registry.register(definition);

        // when
        Optional<BeanEntry<String>> resolved = registry.resolveEntry(String.class, Qualifier.none());

        // then
        assertTrue(resolved.isPresent());
        assertEquals(Qualifier.none(), resolved.get().qualifier());
        assertEquals(definition, resolved.get().definition());
    }

    @Test
    void should_register_and_resolve_definition_when_matching_qualifier_is_used() {
        // given
        JetRegistry registry = new JetRegistry();
        BeanDefinition<String> english = new TestDefinition<>("hello", String.class, Qualifier.of("en"), false);
        BeanDefinition<String> spanish = new TestDefinition<>("hola", String.class, Qualifier.of("es"), false);
        registry.register(english);
        registry.register(spanish);

        // when
        Optional<BeanEntry<String>> resolved = registry.resolveEntry(String.class, Qualifier.of("en"));

        // then
        assertTrue(resolved.isPresent());
        assertEquals(Qualifier.of("en"), resolved.get().qualifier());
        assertEquals(english, resolved.get().definition());
    }

    @Test
    void should_resolve_single_candidate_even_when_candidate_has_qualifier_and_query_has_none() {
        // given
        JetRegistry registry = new JetRegistry();
        BeanDefinition<String> definition = new TestDefinition<>("hello", String.class, Qualifier.of("en"), false);
        registry.register(definition);

        // when
        Optional<BeanEntry<String>> resolved = registry.resolveEntry(String.class, Qualifier.none());

        // then
        assertTrue(resolved.isPresent());
        assertEquals(Qualifier.of("en"), resolved.get().qualifier());
    }

    @Test
    void should_return_empty_when_no_definition_matches_the_given_qualifier() {
        // given
        JetRegistry registry = new JetRegistry();
        registry.register(new TestDefinition<>("hello", String.class, Qualifier.of("en"), false));

        // when
        Optional<BeanEntry<String>> resolved = registry.resolveEntry(String.class, Qualifier.of("fr"));

        // then
        assertTrue(resolved.isEmpty());
    }

    @Test
    void should_return_empty_when_resolving_unregistered_type() {
        // given
        JetRegistry registry = new JetRegistry();

        // when
        Optional<BeanEntry<Integer>> resolved = registry.resolveEntry(Integer.class, Qualifier.none());

        // then
        assertTrue(resolved.isEmpty());
    }

    @Test
    void should_resolve_single_candidate_when_multiple_candidates_with_same_type_and_one_primary() {
        // given
        JetRegistry registry = new JetRegistry();
        registry.register(new TestDefinition<>("hola", String.class, Qualifier.of("es"), false));
        registry.register(new TestDefinition<>("hola primary", String.class, Qualifier.of("es"), true));

        // when
        Optional<BeanEntry<String>> resolved = registry.resolveEntry(String.class, Qualifier.of("es"));

        // then
        assertTrue(resolved.isPresent());
        assertEquals(Qualifier.of("es"), resolved.get().qualifier());
        assertEquals("hola primary", resolved.get().definition().apply(null));
    }

    @Test
    void should_throw_multiple_bean_candidate_exception_when_resolving_type_with_multiple_candidates_and_no_primary() {
        // given
        JetRegistry registry = new JetRegistry();
        registry.register(new TestDefinition<>("hello", String.class, Qualifier.of("en"), false));
        registry.register(new TestDefinition<>("hola", String.class, Qualifier.of("es"), false));

        // when
        MultipleBeanCandidateException exception = assertThrows(
                MultipleBeanCandidateException.class,
                () -> registry.resolveEntry(String.class, Qualifier.none()));

        // then
        assertTrue(exception.getMessage().contains("Multiple candidates found for class java.lang.String"));
    }

    @Test
    void should_throw_multiple_bean_candidate_exception_when_resolving_type_with_multiple_candidates_and_all_primary() {
        // given
        JetRegistry registry = new JetRegistry();
        registry.register(new TestDefinition<>("hello", String.class, Qualifier.of("en"), true));
        registry.register(new TestDefinition<>("hola", String.class, Qualifier.of("es"), true));

        // when
        MultipleBeanCandidateException exception = assertThrows(
                MultipleBeanCandidateException.class,
                () -> registry.resolveEntry(String.class, Qualifier.none()));

        // then
        assertTrue(exception.getMessage().contains("Multiple candidates found for class java.lang.String"));
    }

    @Test
    void should_throw_illegal_argument_exception_when_type_is_null() {
        // given
        JetRegistry registry = new JetRegistry();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.resolveEntry(null, Qualifier.none()));

        // then
        assertTrue(exception.getMessage().contains("Bean type cannot be null"));
    }

    @Test
    void should_throw_illegal_argument_exception_when_qualifier_is_null() {
        // given
        JetRegistry registry = new JetRegistry();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.resolveEntry(String.class, null));

        // then
        assertTrue(exception.getMessage().contains("Qualifier cannot be null"));
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
