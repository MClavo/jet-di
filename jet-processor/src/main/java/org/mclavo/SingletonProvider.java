package org.mclavo;

import java.util.function.Function;

final class SingletonProvider<T> implements ScopeProvider<T> {

    private final Function<BeanProvider, T> delegate;
    private T value;

    SingletonProvider(Function<BeanProvider, T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized T apply(BeanProvider beanProvider) {
        if (value == null) {
            value = delegate.apply(beanProvider);
        }
        return value;
    }
}