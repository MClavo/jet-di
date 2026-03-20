package org.mclavo.context;

import java.util.Objects;

/**
 * Value object used to disambiguate beans of the same type.
 *
 * @param value qualifier text; blank means no qualifier
 */
public record Qualifier(String value) {
    private static final Qualifier NONE = new Qualifier("");

    public Qualifier {
        Objects.requireNonNull(value, "Qualifier value cannot be null");
    }

    /**
     * @return singleton representing no qualifier
     */
    public static Qualifier none() {
        return NONE;
    }

    /**
     * Builds a qualifier from text; blank values map to {@link #none()}.
     *
     * @param value qualifier text
     * @return qualifier value object
     */
    public static Qualifier of(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        return new Qualifier(value);
    }

    /**
     * @return {@code true} when this qualifier represents no qualifier
     */
    public boolean isNone() {
        return value.isBlank();
    }

    @Override
    public String toString() {
        return isNone() ? "<none>" : value;
    }
}
