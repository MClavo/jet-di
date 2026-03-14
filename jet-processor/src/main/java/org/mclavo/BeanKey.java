package org.mclavo;

import java.util.Objects;

public record BeanKey (
    Class<?> type,
    Qualifier qualifier
){
    public BeanKey {
        Objects.requireNonNull(type, "Bean type cannot be null");
        Objects.requireNonNull(qualifier, "Qualifier cannot be null");
    }

    public static BeanKey of(Class<?> type) {
        return new BeanKey(type, Qualifier.none());
    }

    public static BeanKey of(Class<?> type, Qualifier qualifier) {
        return new BeanKey(type, qualifier);
    }

    @Override
    public String toString() {
        return "BeanKey [type=" + type.getName() + ", qualifier=" + qualifier + "]";
    }
}
