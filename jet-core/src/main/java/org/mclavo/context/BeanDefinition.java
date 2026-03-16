package org.mclavo.context;

public interface BeanDefinition<T> {
    BeanKey<T> key();
    T apply(BeanProvider beanProvider);

    default Class<T> type() {
        Class<T> type = (Class<T>) key().type();
        return type;
    }

    default Qualifier qualifier() {
        return key().qualifier();
    }
}
