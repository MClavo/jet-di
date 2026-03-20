package org.mclavo.context;

import java.util.ServiceLoader;

import org.mclavo.exception.BeanDefinitionLoadingException;
import org.mclavo.exception.BeanProvisionException;
import org.mclavo.factory.JetRegistry;

/**
 * Default runtime context that resolves beans from generated definitions.
 */
public class JetContext implements BeanProvider {
    private final JetRegistry registry;

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
     * @param clazz bean class
     * @param <T> bean type
     * @return resolved bean instance
     */
    @Override
    public <T> T provide(Class<T> clazz) {
        T beanFromDefinition = registry.getOrCreateFromDefinition(clazz, this);

        if (beanFromDefinition != null) {
            return beanFromDefinition;
        }

        throw new BeanProvisionException(
                "No BeanDefinition found for " + clazz.getName()
                        + ". Make sure it is generated and published through ServiceLoader.");
    }

    /**
     * Qualifier-aware lookup is currently not implemented in registry resolution.
     */
    @Override
    public <T> T provide(Class<T> key, String qualifier) {
        return provide(key);
    }


    /**
     * Qualifier-aware lookup is currently not implemented in registry resolution.
     */
    @Override
    public <T> T provide(Class<T> beanType, Qualifier qualifier) {
        return provide(beanType);
    }
}
