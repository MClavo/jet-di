package org.mclavo;

import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Part;

@Hangar
public class MyHangar {
    
    @Part
    public SimplePart PartMessage() {
        return new SimplePart("HANGAR TEST");
    }


}
