package org.mclavo;

import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Part;

@Hangar
public class MyHangar {
    
    @Part
    public BeanWrapper PartMessage(EmptyBean emptyBean) {
        return new BeanWrapper(emptyBean);
    }


}
