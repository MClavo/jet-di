package org.mclavo.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;
import org.mclavo.context.BeanProvider;
import org.mclavo.exception.BeanInstantiationException;

public final class JetFactory {

    private final JetRegistry registry;
    private final BeanProvider beanProvider;

    public JetFactory(JetRegistry registry, BeanProvider beanProvider) {
        this.registry = registry;
        this.beanProvider = beanProvider;
    }

    public <T> T getInstanceOf(Class<T> beanClass, Object... arguments) {
        try {
            T beanFromDefinition = registry.getOrCreateFromDefinition(beanClass, beanProvider);

            if (beanFromDefinition != null) {
                return beanFromDefinition;
            }

            if (beanClass.isAnnotationPresent(Jet.class)) {

                if (registry.contains(beanClass)) {
                    return beanClass.cast(registry.get(beanClass));
                }
                
                T bean = instantiateBeanClass(beanClass, arguments);
                registry.register(beanClass, bean);
                
                /*
                 * Note that the call to ConcurrentMap.putIfAbsent() is an atomic call
                 * in the case of ConcurrentMap, so you do not need to synchronize this
                 * part.
                 * 
                 * There is one caveat though: the value you passed to the
                 * ConcurrentMap.putIfAbsent() may not be the one that, in the end,
                 * was put in the map. That could be the case if several threads called
                 * this ConcurrentMap.putIfAbsent() concurrently, with different instances.
                 * In the end, there is a winner, and it could be another thread than yours.
                 * So to overcome this, you need to call ConcurrentMap.get(), to return the
                 * singleton.
                 * 
                 * REF: (https://dev.java/learn/reflection/dependency-injection/)
                 */
                return beanClass.cast(registry.get(beanClass));

            } else {
                T bean = instantiateBeanClass(beanClass, arguments);

                return bean;
            }

        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException
                | IllegalAccessException e) {
            throw new BeanInstantiationException(
                    "Failed to instantiate bean of type " + beanClass.getName(),
                    e);
        }

    }

    private <T> T instantiateBeanClass(Class<T> beanClass, Object[] arguments)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {

        Class<?>[] argumentClasses = Arrays.stream(arguments)
                .map(Object::getClass)
                .toArray(Class<?>[]::new);

        Constructor<T> beanConstructor = beanClass.getConstructor(argumentClasses);

        T bean = beanConstructor.newInstance(arguments);

        Field[] fields = beanClass.getDeclaredFields();
        
        // Search for methods with Intake annotation that need to be injected
        Field[] injectableFields = Arrays.stream(fields)
                .filter(f -> f.isAnnotationPresent(Intake.class))
                .toArray(Field[]::new);
        

        for(Field f : injectableFields) {
            // Getting the class of this field, 
            // and creating an instance of this class
            // using BeanFactory
            Class<?> fieldClass = f.getType();
            Object fieldValue = getInstanceOf(fieldClass);

            // Injecting
            f.setAccessible(true);
            f.set(bean, fieldValue);
        }
        
        return bean;
    }

}
