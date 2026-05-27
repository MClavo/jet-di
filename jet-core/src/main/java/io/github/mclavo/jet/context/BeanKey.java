package io.github.mclavo.jet.context;

import java.util.Objects;

/**
 * Immutable identifier for a bean registration.
 *
 * @param type bean class
 * @param qualifier qualifier used to disambiguate beans of the same type
 * @param <T> bean type
 */
public final record BeanKey<T> (
    Class<T> type,
    Qualifier qualifier
){
    public BeanKey {
        Objects.requireNonNull(type, "Bean type cannot be null");
        Objects.requireNonNull(qualifier, "Qualifier cannot be null");
    }

    /**
     * Creates a key with no qualifier.
     *
     * @param type bean class
     * @param <T> bean type
     * @return key with {@link Qualifier#none()}
     */
    public static <T> BeanKey<T> of(Class<T> type) {
        return new BeanKey<>(type, Qualifier.none());
    }

    /**
     * Creates a key with an explicit qualifier.
     *
     * @param type bean class
     * @param qualifier bean qualifier
     * @param <T> bean type
     * @return qualified bean key
     */
    public static <T> BeanKey<T> of(Class<T> type, Qualifier qualifier) {
        return new BeanKey<>(type, qualifier);
    }

    @Override
    public String toString() {
        return "BeanKey[type=" + type.getName() + ", qualifier=" + qualifier + "]";
    }
}
