package org.mclavo.context;

import java.util.ServiceLoader;

import org.mclavo.exception.BeanDefinitionLoadingException;
import org.mclavo.exception.BeanProvisionException;
import org.mclavo.factory.JetRegistry;

public class JetContext implements BeanProvider {
    private JetRegistry registry;

    JetContext() {
        this.registry = new JetRegistry();

        loadBeanDefinitions(Thread.currentThread().getContextClassLoader());

    }

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

    @Override
    public <T> T provide(Class<T> key, String qualifier) {
        // Qualifier support is reserved for BeanKey-based lookup in a next iteration.
        return provide(key);
    }


    @Override
    public <T> T provide(Class<T> beanType, Qualifier qualifier) {
        // Qualifier support is reserved for BeanKey-based lookup in a next iteration.
        return provide(beanType);
    }
}
