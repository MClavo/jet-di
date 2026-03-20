package org.mclavo.context;

/**
 * Read-only bean lookup contract.
 */
public interface BeanResolver {
    /**
     * @param key bean class
     * @param <T> bean type
     * @return resolved bean instance
     */
    <T> T get(Class<T> key);

    /**
     * @param key bean class
     * @param qualifier textual qualifier
     * @param <T> bean type
     * @return resolved bean instance
     */
    <T> T get(Class<T> key, String qualifier);
}
