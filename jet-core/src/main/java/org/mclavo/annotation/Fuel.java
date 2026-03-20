package org.mclavo.annotation;

/**
 * Declares a qualifier value for a bean provided by a {@code @Part} method.
 */
public @interface Fuel {
    /**
     * @return qualifier name associated with the produced bean
     */
    String value() default "";
}
