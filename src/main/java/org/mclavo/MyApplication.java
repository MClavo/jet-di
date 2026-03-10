package org.mclavo;

import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;

@Jet
public class MyApplication {
    
    @Intake
    private Service service;

    public boolean isServiceSet() {
        return service != null;
    }
}
