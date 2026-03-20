package org.mclavo.context;

/**
 * Resolves bean instances from the active context.
 */
public interface BeanProvider {
    /**
     * @param beanType bean class to resolve
     * @param <T> bean type
     * @return resolved bean instance
     */
    <T> T provide(Class<T> beanType);

    /**
     * @param beanType bean class to resolve
     * @param qualifier qualifier associated with the bean
     * @param <T> bean type
     * @return resolved bean instance
     */
    <T> T provide(Class<T> beanType, Qualifier qualifier);

    /**
     * Convenience overload that converts a qualifier string into a {@link Qualifier}.
     *
     * @param beanType bean class to resolve
     * @param qualifier textual qualifier
     * @param <T> bean type
     * @return resolved bean instance
     */
    default <T> T provide(Class<T> beanType, String qualifier) {
        return provide(beanType, Qualifier.of(qualifier));
    }
}
