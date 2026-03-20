package org.mclavo.factory;

/**
 * Factory placeholder for future hangar-specific creation and lifecycle concerns.
 */
public final class HangarFactory {
    private final JetRegistry registry;

    /**
     * @param registry shared bean registry
     */
    public HangarFactory(JetRegistry registry) {
        this.registry = registry;
    }
}
