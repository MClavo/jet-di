package org.mclavo;

import java.util.function.Function;

public interface ScopeProvider<T> extends Function<BeanProvider, T> {

    static <T> ScopeProvider<T> singletonScope(Function<BeanProvider, T> delegate) {
        return new SingletonProvider<>(delegate);
    }

    static <T> ScopeProvider<T> prototypeScope(Function<BeanProvider, T> delegate) {
        return delegate::apply;
    }
}
