package org.mclavo.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.mclavo.exception.BeanDefinitionLoadingException;
import org.mclavo.exception.BeanProvisionException;
import org.mclavo.exception.CircularDependencyException;
import org.mclavo.factory.JetRegistry;

/**
 * Default runtime context that resolves beans from generated definitions.
 */
public class JetContext implements BeanProvider {
    private final JetRegistry registry;
    private final ThreadLocal<Deque<BeanKey<?>>> resolutionStack = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Creates a context and eagerly loads {@code BeanDefinition} services.
     */
    JetContext() {
        this.registry = new JetRegistry();

        loadBeanDefinitions(Thread.currentThread().getContextClassLoader());

    }

    /**
     * Loads generated definition providers using Java ServiceLoader.
     *
     * @param classLoader class loader used to discover service entries
     */
    private void loadBeanDefinitions(ClassLoader classLoader) {
        @SuppressWarnings("rawtypes")
        ServiceLoader<BeanDefinition> definitionLoader = ServiceLoader.load(BeanDefinition.class, classLoader);

        try {
            for (BeanDefinition<?> definition : definitionLoader) {
                System.out.println("Loaded BeanDefinition: " + definition.getClass().getName());
                registry.register(definition);
            }

        } catch (Exception e) {
            throw new BeanDefinitionLoadingException(
                "Failed to load BeanDefinitions from ServiceLoader",
                e);
        }
    }

    /**
     * @param beanClass bean class
     * @param <T> bean type
     * @return resolved bean instance
     */
    @Override
    public <T> T provide(Class<T> beanClass) {
        return resolve(BeanKey.of(beanClass));
    }

    /**
     * Resolves a bean using the complete key (type + qualifier).
     */
    private <T> T resolve(BeanKey<T> key) {
        T existing = registry.get(key);
        if (existing != null) {
            return existing;
        }

        Deque<BeanKey<?>> stack = resolutionStack.get();
        if (stack.contains(key)) {
            throw new CircularDependencyException(buildCircularDependencyMessage(stack, key));
        }

        stack.push(key);
        try {
            BeanDefinition<T> definition = registry.getDefinition(key);
            if (definition != null) {
                T created = definition.apply(this);
                registry.register(key, created);
                // Return the canonical singleton instance when concurrent creation races happen.
                return registry.get(key);
            }

            throw new BeanProvisionException(
                "No BeanDefinition found for " + key
                + ". Make sure it is generated and published through ServiceLoader.");

        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                resolutionStack.remove();
            }
        }
    }

    private String buildCircularDependencyMessage(Deque<BeanKey<?>> stack, BeanKey<?> repeatedKey) {
        List<BeanKey<?>> path = new ArrayList<>(stack);
        Collections.reverse(path);
        path.add(repeatedKey);

        return "Circular dependency detected: "
            + path.stream().map(BeanKey::toString).collect(Collectors.joining(" -> "));
    }

    
    @Override
    public <T> T provide(Class<T> key, String qualifier) {
        return provide(key, Qualifier.of(qualifier));
    }

    @Override
    public <T> T provide(Class<T> beanType, Qualifier qualifier) {
        return resolve(BeanKey.of(beanType, qualifier));
    }
}
