package org.mclavo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a qualifier value for a bean provided by a {@link Part} method.
 */
@Target({
    ElementType.METHOD,
    ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
public @interface Fuel {
    /**
     * @return qualifier name associated with the produced bean
     */
    String value() default "";
}
