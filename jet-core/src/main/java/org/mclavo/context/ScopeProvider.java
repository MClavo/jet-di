package org.mclavo.context;

import java.util.function.Function;

/**
 * Scope strategy used by generated bean definitions.
 *
 * @param <T> bean type produced by this scope strategy
 */
public interface ScopeProvider<T> extends Function<BeanProvider, T> {

    /**
     * Creates a singleton scope backed by synchronized lazy initialization.
     *
     * @param delegate creation function
     * @param <T> bean type
     * @return singleton scope provider
     */
    static <T> ScopeProvider<T> singletonScope(Function<BeanProvider, T> delegate) {
        return new SingletonProvider<>(delegate);
    }

    /**
     * Creates a prototype scope that builds a new instance on each call.
     *
     * @param delegate creation function
     * @param <T> bean type
     * @return prototype scope provider
     */
    static <T> ScopeProvider<T> prototypeScope(Function<BeanProvider, T> delegate) {
        return delegate::apply;
    }
}
