package org.mclavo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Loadout
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Intake {
    
}
