package org.mclavo.context;

import org.mclavo.factory.HangarFactory;
import org.mclavo.factory.JetFactory;
import org.mclavo.factory.JetRegistry;

public class JetContext {
    private JetRegistry registry;
    private JetFactory jetFactory;
    private HangarFactory hangarFactory;

    JetContext() {
        this.registry = new JetRegistry();
        this.jetFactory = new JetFactory(registry);
        this.hangarFactory = new HangarFactory(registry);
    }

    public <T> void scan(Class<T> clazz) {
        
    }


    public <T> T get(Class<T> clazz) {
        return jetFactory.getInstanceOf(clazz);
    }
}
