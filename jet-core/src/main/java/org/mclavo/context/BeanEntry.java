package org.mclavo.context;

/**
 * Represents an entry in the bean registry, encapsulating all metadata and state
 * required for bean management and resolution.
 * <p>
 * Each {@code BeanEntry} holds:
 * <ul>
 *   <li>The {@link BeanDefinition} describing the bean's type, creation strategy, and configuration.</li>
 *   <li>An optional {@link Qualifier} for distinguishing between multiple candidates of the same type.</li>
 *   <li>The current bean instance, if it has been instantiated (may be {@code null} for lazy or prototype beans).</li>
 * </ul>
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintain metadata and qualifiers for candidate resolution.</li>
 *   <li>Track the bean's lifecycle state and instance.</li>
 *   <li>Support flexible instantiation strategies.</li>
 * </ul>
 *
 * @param <T> the type of the bean managed by this entry
 */
final class BeanEntry<T> {
    private final BeanDefinition<T> definition;
    
    // Scope ?
    private volatile T instance; // nullable for lazy singletons or prototypes

    BeanEntry(BeanDefinition<T> definition) {
        this.definition = definition;
    }

    BeanDefinition<T> definition() {
        return definition;
    }

    Qualifier qualifier() {
        return definition.qualifier();
    }


    boolean isPrimary() {
        return definition.primary();
    }

    T instance() {
        return instance;
    }

    void initialize(T instance) {
        this.instance = instance;
    }
}
