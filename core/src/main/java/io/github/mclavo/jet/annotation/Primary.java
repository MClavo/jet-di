package io.github.mclavo.jet.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the bean provided by a {@link Bean} method should be
 * considered the primary candidate for injection when multiple beans of the
 * same type are available.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Primary {
}
