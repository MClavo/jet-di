package org.mclavo.context;

import java.util.Objects;

public record BeanKey<T> (
    Class<T> type,
    Qualifier qualifier
){
    public BeanKey {
        Objects.requireNonNull(type, "Bean type cannot be null");
        Objects.requireNonNull(qualifier, "Qualifier cannot be null");
    }

    public static <T> BeanKey<T> of(Class<T> type) {
        return new BeanKey<>(type, Qualifier.none());
    }

    public static <T> BeanKey<T> of(Class<T> type, Qualifier qualifier) {
        return new BeanKey<>(type, qualifier);
    }

    @Override
    public String toString() {
        return "BeanKey[type=" + type.getName() + ", qualifier=" + qualifier + "]";
    }
}
