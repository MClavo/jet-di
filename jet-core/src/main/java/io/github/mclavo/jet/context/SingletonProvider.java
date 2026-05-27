package io.github.mclavo.jet.context;

import java.util.function.Function;

/**
 * Thread-safe singleton scope implementation.
 *
 * @param <T> bean type
 */
final class SingletonProvider<T> implements ScopeProvider<T> {

    private final Function<BeanProvider, T> delegate;
    private T value;

    SingletonProvider(Function<BeanProvider, T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Synchronization ensures only one instance is created when multiple threads race.
     */
    @Override
    public synchronized T apply(BeanProvider beanProvider) {
        if (value == null) {
            value = delegate.apply(beanProvider);
        }
        return value;
    }
}