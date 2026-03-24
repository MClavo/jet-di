package org.mclavo.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mclavo.context.BeanDefinition;
import org.mclavo.context.BeanKey;
import org.mclavo.context.Qualifier;
import org.mclavo.exception.DuplicateBeanDefinitionException;

/**
 * Concurrent registry for instantiated beans and generated bean definitions.
 */
public class JetRegistry {
    private final ConcurrentMap<BeanKey<?>, Object> registry = new ConcurrentHashMap<>();
    private final ConcurrentMap<BeanKey<?>, BeanDefinition<?>> definitions = new ConcurrentHashMap<>();
    
    /**
     * Registers a bean singleton instance if not already present.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register(Class<?> clazz, Object instance) {
        register((BeanKey) BeanKey.of(clazz), instance);
    }

    /**
     * Registers a bean singleton instance for a qualified key if not already present.
     */
    public <T> void register(Class<T> clazz, Qualifier qualifier, T instance) {
        register(BeanKey.of(clazz, qualifier), instance);
    }

    /**
     * Registers a bean singleton instance for the provided key if not already present.
     */
    public <T> void register(BeanKey<T> key, T instance) {
        registry.putIfAbsent(key, instance);
    }

    /**
     * Registers a generated definition if not already present.
     */
    public void register(BeanDefinition<?> definition) {
        BeanKey<?> key = definition.key();
        if (key == null) {
            throw new IllegalArgumentException(
                "BeanDefinition key cannot be null for " + definition.getClass().getName());
        }

        BeanDefinition<?> existing = definitions.putIfAbsent(key, definition);
        if (existing != null && existing != definition) {
            throw new DuplicateBeanDefinitionException(
                "Duplicate BeanDefinition for " + key + ": "
                + existing.getClass().getName() + " and " + definition.getClass().getName());
        }
    }

    /**
     * Returns a registered definition for the class or {@code null} if missing.
     */
    @SuppressWarnings("unchecked")
    public <T> BeanDefinition<T> getDefinition(Class<T> clazz) {
        return (BeanDefinition<T>) definitions.get(BeanKey.of(clazz));
    }

    /**
     * Returns a registered definition for the class/qualifier or {@code null} if missing.
     */
    @SuppressWarnings("unchecked")
    public <T> BeanDefinition<T> getDefinition(Class<T> clazz, Qualifier qualifier) {
        return (BeanDefinition<T>) definitions.get(BeanKey.of(clazz, qualifier));
    }

    /**
     * Returns a registered definition for the key or {@code null} if missing.
     */
    @SuppressWarnings("unchecked")
    public <T> BeanDefinition<T> getDefinition(BeanKey<T> key) {
        return (BeanDefinition<T>) definitions.get(key);
    }

    /**
     * Returns a registered bean instance or {@code null} if missing.
     */
    public <T> T get(Class<T> clazz) {
        return get(BeanKey.of(clazz));
    }

    /**
     * Returns a registered bean instance by class and qualifier, or {@code null} if missing.
     */
    public <T> T get(Class<T> clazz, Qualifier qualifier) {
        return get(BeanKey.of(clazz, qualifier));
    }

    /**
     * Returns a registered bean instance by key, or {@code null} if missing.
     */
    public <T> T get(BeanKey<T> key) {
        return key.type().cast(registry.get(key));
    }

    /**
     * @return {@code true} when a singleton instance for the class is already registered
     */
    public <T> boolean contains(Class<T> clazz) {
        return contains(BeanKey.of(clazz));
    }

    /**
     * @return {@code true} when a singleton instance for class/qualifier is registered
     */
    public <T> boolean contains(Class<T> clazz, Qualifier qualifier) {
        return contains(BeanKey.of(clazz, qualifier));
    }

    /**
     * @return {@code true} when a singleton instance for the key is already registered
     */
    public <T> boolean contains(BeanKey<T> key) {
        return registry.containsKey(key);
    }
}
