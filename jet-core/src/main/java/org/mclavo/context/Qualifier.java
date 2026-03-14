package org.mclavo.context;

import java.util.Objects;

public record Qualifier(String value) {
    private static final Qualifier NONE = new Qualifier("");

    public Qualifier {
        Objects.requireNonNull(value, "Qualifier value cannot be null");
    }

    public static Qualifier none() {
        return NONE;
    }

    public static Qualifier of(String value) {
        if(value == null || value.isBlank()) {
            return NONE;
        }

        return new Qualifier(value);
    }

    public boolean isNone() {
        return value.isBlank();
    }

    @Override
    public String toString() {
        return isNone() ? "<none>" : value;
    }
}
