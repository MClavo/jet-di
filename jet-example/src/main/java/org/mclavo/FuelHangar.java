package org.mclavo;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Maverick;
import org.mclavo.annotation.Part;

@Hangar
public class FuelHangar {
    
    @Part
    @Fuel("Gasoline")
    public FuelComponent generateGasolinePart() {
        return new FuelComponent("Gasoline");
    }

    @Part
    @Fuel("Diesel")
    public FuelComponent generateDieselPart() {
        return new FuelComponent("Diesel");
    }

    @Part
    @Fuel("Diesel")
    @Maverick
    public FuelComponent generatePrimaryDieselPart() {
        return new FuelComponent("Turbo Diesel");
    }

}
