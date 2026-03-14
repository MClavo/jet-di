package org.mclavo.context;

public interface BeanResolver {
    <T> T get(Class<T> key);
    <T> T get(Class<T> key, String qualifier);
}
