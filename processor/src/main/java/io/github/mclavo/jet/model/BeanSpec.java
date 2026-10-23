package io.github.mclavo.jet.model;

import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;


/**
 * Represents a binding.
 * 
 * @param beanType the type of the bean
 * @param qualifier the normalized qualifier of the bean
 * @param primary whether the bean is primary
 * @param creation the creation strategy
 * @param origin the origin element
 */
public record BeanSpec(
    TypeMirror beanType,
    String qualifier,
    boolean primary,
    CreationSpec creation,
    Element origin
) {
    public BeanSpec {
        Objects.requireNonNull(beanType);
        Objects.requireNonNull(qualifier);
        Objects.requireNonNull(creation);
        Objects.requireNonNull(origin);
    }
}