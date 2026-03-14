package org.mclavo.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class JetRegistry {
    private final ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<>();
    
    public void register(Class<?> clazz, Object instance) {
        registry.putIfAbsent(clazz, instance);
    }

    public <T> T get(Class<T> clazz) {
        return clazz.cast(registry.get(clazz));
    }

    public <T> boolean contains(Class<T> clazz) {
        return registry.containsKey(clazz);
    }
}
