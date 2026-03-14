package org.mclavo;

import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Part;

@Hangar
public final class MyFactory {
    @Part
    Service TestService() {
        return new Service("Hangar Service");
    }
}
