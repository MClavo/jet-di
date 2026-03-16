package org.mclavo.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mclavo.context.BeanDefinition;
import org.mclavo.context.BeanProvider;

public class JetRegistry {
    private final ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, BeanDefinition<?>> definitions = new ConcurrentHashMap<>();
    
    public void register(Class<?> clazz, Object instance) {
        registry.putIfAbsent(clazz, instance);
    }

    public void register(BeanDefinition<?> definition) {
        definitions.putIfAbsent(definition.type(), definition);
    }

    public <T> T getOrCreateFromDefinition(Class<T> clazz, BeanProvider beanProvider) {
        Object existing = registry.get(clazz);

        if (existing != null) {
            return clazz.cast(existing);
        }

        BeanDefinition<?> definition = definitions.get(clazz);

        if (definition == null) {
            return null;
        }

        Object created = definition.apply(beanProvider);
        registry.putIfAbsent(clazz, created);

        return clazz.cast(registry.get(clazz));
    }

    public <T> T get(Class<T> clazz) {
        return clazz.cast(registry.get(clazz));
    }

    public <T> boolean contains(Class<T> clazz) {
        return registry.containsKey(clazz);
    }
}
