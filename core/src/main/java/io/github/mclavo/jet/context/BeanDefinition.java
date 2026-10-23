package io.github.mclavo.jet.context;

/**
 * Contract implemented by generated metadata capable of creating a bean instance.
 *
 * @param <T> bean type handled by this definition
 */
public interface BeanDefinition<T> {

    /**
     * @return the type of the bean handled by this definition
     */
    Class<T> type();

    /**
     * @return the qualifier of the bean handled by this definition
     */
    Qualifier qualifier();

    /**
     * @return whether this bean is primary or not
     */
    boolean primary();

    /**
     * Creates a bean instance, resolving dependencies through the supplied provider.
     * Singleton caching is managed by {@link JetContext}, not by generated definitions.
     *
     * @param beanProvider provider used to resolve dependencies
     * @return created bean instance
     */
    T apply(BeanProvider beanProvider);
}
