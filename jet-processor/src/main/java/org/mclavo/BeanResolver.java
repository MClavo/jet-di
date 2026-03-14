package org.mclavo;

public interface BeanResolver {
    <T> T get(Class<T> key);
    <T> T get(Class<T> key, String qualifier);
}
