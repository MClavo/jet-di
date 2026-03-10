package org.mclavo.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;

public enum JetFactory {
    INSTANCE;

    private final ConcurrentMap<Class<?>, Object> registry = new ConcurrentHashMap<>();

    public <T> T getInstanceOf(Class<T> beanClass, Object... arguments) {
        try {
            if (beanClass.isAnnotationPresent(Jet.class)) {

                if (registry.containsKey(beanClass)) {
                    return beanClass.cast(registry.get(beanClass));
                }
                
                T bean = instantiateBeanClass(beanClass, arguments);
                registry.putIfAbsent(beanClass, bean);
                
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
            throw new RuntimeException(e);
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
            Object fieldValue = JetFactory.INSTANCE.getInstanceOf(fieldClass);

            // Injecting
            f.setAccessible(true);
            f.set(bean, fieldValue);
        }
        
        return bean;
    }

}
