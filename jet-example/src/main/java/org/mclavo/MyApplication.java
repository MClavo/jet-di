package org.mclavo;

import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;

@Jet
public class MyApplication {
    
    private final SimpleService service;

    @Intake
    public MyApplication(SimpleService service) {
        this.service = service;
    }

    public boolean isServiceSet() {
        return service != null;
    }
}
