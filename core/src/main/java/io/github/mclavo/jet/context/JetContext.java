package io.github.mclavo.jet.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import io.github.mclavo.jet.exception.BeanDefinitionLoadingException;
import io.github.mclavo.jet.exception.BeanProvisionException;
import io.github.mclavo.jet.exception.CircularDependencyException;

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

    @Override
    public <T> T provide(Class<T> beanClass) {
        return resolve(beanClass, Qualifier.none());
    }

    @Override
    public <T> T provide(Class<T> beanType, Qualifier qualifier) {
        return resolve(beanType, qualifier);
    }

    @Override
    public <T> T provide(Class<T> key, String qualifier) {
        return provide(key, Qualifier.of(qualifier));
    }


    // ────────────────────────── Private Methods ──────────────────────────

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
                //System.out.println("Loaded BeanDefinition: " + definition.getClass().getName());
                registry.register(definition);
            }

        } catch (Exception e) {
            throw new BeanDefinitionLoadingException(
                    "Failed to load BeanDefinitions from ServiceLoader",
                    e);
        }
    }


    /**     
     * Core resolution method that handles bean lookup, instantiation, and circular
     * dependency detection.
     *
     * @param beanType  class of the requested bean
     * @param qualifier optional qualifier for disambiguation
     * @return resolved bean instance
     */
    private <T> T resolve(Class<T> beanType, Qualifier qualifier) {
        validateQualifierAndType(beanType, qualifier);
        
        Optional<BeanEntry<T>> entryOpt = registry.resolveEntry(beanType, qualifier);
        if (entryOpt.isEmpty()) {
            throw new BeanProvisionException(buildBeanProvisionMessage(beanType, qualifier));
        }

        BeanEntry<T> entry = entryOpt.get();

        T existing = entry.instance();
        if (existing != null) {
            return existing;
        }

        Deque<BeanKey<?>> stack = resolutionStack.get();
        BeanKey<T> key = BeanKey.of(beanType, qualifier);
        if (stack.contains(key)) {
            throw new CircularDependencyException(buildCircularDependencyMessage(stack, key));
        }

        stack.push(key);
        try {
            synchronized (entry) {
                T instance = entry.instance();
                if (instance == null) {
                    instance = entry.definition().apply(this);
                    entry.initialize(instance);
                }

                return instance;
            }

        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                resolutionStack.remove();
            }
        }
    }

    private <T> void validateQualifierAndType(Class<T> type, Qualifier qualifier) {
        if(type == null) {
            throw new IllegalArgumentException("Bean type cannot be null");
        }

        if (qualifier == null) {
            throw new IllegalArgumentException("Qualifier cannot be null; use Qualifier.none() for no qualifier");
        }
    }


    //  ────────────────────────── Error Messages Builders ──────────────────────────

    private <T> String buildBeanProvisionMessage(Class<T> beanType, Qualifier qualifier) {
        StringBuilder message = new StringBuilder("No bean candidate found for ")
                .append(beanType.getName());
        if (qualifier != null && !qualifier.isNone()) {
            message.append(" with qualifier '").append(qualifier).append("'");
        }
        return message.toString();
    }

    private String buildCircularDependencyMessage(Deque<BeanKey<?>> stack, BeanKey<?> repeatedKey) {
        List<BeanKey<?>> path = new ArrayList<>(stack);
        Collections.reverse(path);
        path.add(repeatedKey);

        return "Circular dependency detected: "
                + path.stream().map(BeanKey::toString).collect(Collectors.joining(" -> "));
    }


}
