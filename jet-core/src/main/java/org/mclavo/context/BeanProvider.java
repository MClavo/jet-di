package org.mclavo.context;

public interface BeanProvider {
    <T> T provide(Class<T> beanType);

    <T> T provide(Class<T> beanType, Qualifier qualifier);

    default <T> T provide(Class<T> beanType, String qualifier) {
        return provide(beanType, Qualifier.of(qualifier));
    }
}
