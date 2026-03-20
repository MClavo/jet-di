package org.mclavo.context;

/**
 * Contract implemented by generated metadata capable of creating a bean instance.
 *
 * @param <T> bean type handled by this definition
 */
public interface BeanDefinition<T> {
    /**
     * @return key that uniquely identifies the bean type and qualifier
     */
    BeanKey<T> key();

    /**
     * Builds or resolves the bean instance.
     *
     * @param beanProvider provider used to resolve dependencies
     * @return resolved bean instance
     */
    T apply(BeanProvider beanProvider);

    /**
     * @return bean type declared by this definition key
     */
    default Class<T> type() {
        Class<T> type = (Class<T>) key().type();
        return type;
    }

    /**
     * @return bean qualifier declared by this definition key
     */
    default Qualifier qualifier() {
        return key().qualifier();
    }
}
