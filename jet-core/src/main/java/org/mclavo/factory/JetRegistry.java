package org.mclavo.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mclavo.context.BeanDefinition;
import org.mclavo.context.BeanProvider;

/**
 * Concurrent registry for instantiated beans and generated bean definitions.
 */
public class JetRegistry {
    private final ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, BeanDefinition<?>> definitions = new ConcurrentHashMap<>();
    
    /**
     * Registers a bean singleton instance if not already present.
     */
    public void register(Class<?> clazz, Object instance) {
        registry.putIfAbsent(clazz, instance);
    }

    /**
     * Registers a generated definition if not already present.
     */
    public void register(BeanDefinition<?> definition) {
        definitions.putIfAbsent(definition.type(), definition);
    }

    /**
     * Returns an existing bean instance or creates one from a definition when available.
     */
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

    /**
     * Returns a registered bean instance or {@code null} if missing.
     */
    public <T> T get(Class<T> clazz) {
        return clazz.cast(registry.get(clazz));
    }

    /**
     * @return {@code true} when a singleton instance for the class is already registered
     */
    public <T> boolean contains(Class<T> clazz) {
        return registry.containsKey(clazz);
    }
}
