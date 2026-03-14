package org.mclavo.context;

public interface BeanDefinition<T> {
    BeanKey key();
    T create(BeanProvider beanProvider);

    default Class<T> type() {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) key().type();
        return type;
    }

    default Qualifier qualifier() {
        return key().qualifier();
    }
}
