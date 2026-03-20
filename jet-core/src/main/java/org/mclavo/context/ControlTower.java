package org.mclavo.context;

/**
 * Entry point facade used to bootstrap a {@link JetContext} instance.
 */
public final class ControlTower {
    /**
     * Starts the container and returns a ready-to-use context.
     *
     * @param bootClass application bootstrap class
     * @return initialized context
     */
    public static JetContext run(Class<?> bootClass) {
        JetContext context = new JetContext();

        return context;
    }
}
