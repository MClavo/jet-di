package org.mclavo.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mclavo.exception.DuplicateBeanDefinitionException;
import org.mclavo.exception.MultipleBeanCandidateException;

/**
 * Concurrent registry for instantiated beans and generated bean definitions.
 */
final class JetRegistry {
    /*
     * List<BeanEntry<?>> is simpler to manage that a map, as this frameworks is not
     * designed for
     * high performance or large bean counts, and it allows more flexible resolution
     * strategies.
     */
    private final ConcurrentMap<Class<?>, List<BeanEntry<?>>> registry = new ConcurrentHashMap<>();


    // ────────────────────────── Register ──────────────────────────

    /**
     * Registers a bean definition, ensuring no duplicates for the same type and
     * qualifier.
     */
    void register(BeanDefinition<?> definition) {

        if (!registry.containsKey(definition.type())) {
            List<BeanEntry<?>> entries = new ArrayList<>();
            BeanEntry<?> entry = new BeanEntry<>(definition);
            entries.add(entry);
            registry.put(definition.type(), entries);

        } else {
            List<BeanEntry<?>> entries = registry.get(definition.type());
            for (BeanEntry<?> entry : entries) {
                if (entry.qualifier().equals(definition.qualifier())) {
                    throw new DuplicateBeanDefinitionException("Duplicate Bean for "
                            + definition.type() + " with qualifier " + definition.qualifier());
                }
            }

            BeanEntry<?> entry = new BeanEntry<>(definition);
            entries.add(entry);
        }
    }



    // ────────────────────────── Resolve ──────────────────────────
    
    /**
     * Resolves a bean entry for the given type and qualifier, applying resolution
     * rules:
     * <ul>
     *   <li>If no qualifier is provided, returns the unique entry for the type, or the primary if multiple exist.</li>
     *   <li>If a qualifier is provided, returns the unique entry matching the type and qualifier, or the primary among them if multiple exist.</li>
     * </ul>
     *
     * @param type      the class of the bean to resolve
     * @param qualifier the qualifier to distinguish between candidates (use {@link Qualifier#none()} for no qualifier)
     * @param <T>       the type of the bean
     * @return an Optional containing the resolved BeanEntry, or empty if no match is found
     * @throws MultipleBeanCandidateException if multiple candidates are found without a unique primary
     */
    <T> Optional<BeanEntry<T>> resolveEntry(Class<T> type, Qualifier qualifier) {  
        validateQualifierAndType(type, qualifier);

        if (qualifier.equals(Qualifier.none())) {
            return Optional.ofNullable(getBeanEntry(type));
        }

        return Optional.ofNullable(getBeanEntry(type, qualifier));

    }


    // ────────────────────────── Private Methods ──────────────────────────

    @SuppressWarnings("unchecked")
    private <T> BeanEntry<T> getBeanEntry(Class<T> type) {
        validateQualifierAndType(type, Qualifier.none());

        List<BeanEntry<T>> entries = (List<BeanEntry<T>>) (List<?>) registry.get(type);
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        // If only one candidate exists, return it directly, even if it is not marked as
        // primary or has a qualifier. This allows simple cases to work without extra
        // configuration.
        if (entries.size() == 1) {
            return entries.get(0);
        }

        List<BeanEntry<T>> primaries = entries.stream()
                .filter(BeanEntry::isPrimary)
                .toList();

        if (primaries.size() == 1) {
            return primaries.get(0);
        }

        throw new MultipleBeanCandidateException("Multiple candidates found for " + type);    
    }

    @SuppressWarnings("unchecked")
    private <T> BeanEntry<T> getBeanEntry(Class<T> type, Qualifier qualifier) {
        validateQualifierAndType(type, qualifier);

        List<BeanEntry<T>> entries = (List<BeanEntry<T>>) (List<?>) registry.get(type);
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        List<BeanEntry<T>> qualifiedEntries = entries.stream()
                .filter(entry -> entry.qualifier().equals(qualifier))
                .toList();

        if (qualifiedEntries.isEmpty()) {
            return null;
        }

        if (qualifiedEntries.size() == 1) {
            return qualifiedEntries.get(0);
        }

        // Multiple candidates with the same qualifier, try to find a unique primary among them.
        List<BeanEntry<T>> primaries = qualifiedEntries.stream()
                .filter(BeanEntry::isPrimary)
                .toList();

        if (primaries.size() == 1) {
            return primaries.get(0);
        }

        // Multiple candidates with the same qualifier, also be multiple primaries, but
        // the error is the same: no unique candidate.
        throw new MultipleBeanCandidateException(
                "Multiple candidates found for " + type + " with qualifier " + qualifier);
    
    }

    private <T> void validateQualifierAndType(Class<T> type, Qualifier qualifier) {
        if(type == null) {
            throw new IllegalArgumentException("Bean type cannot be null");
        }

        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier cannot be null; use Qualifier.none() for no qualifier");
        }
    }
}
