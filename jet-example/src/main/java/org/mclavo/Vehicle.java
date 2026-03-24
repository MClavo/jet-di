package org.mclavo;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;

@Jet
public final class Vehicle {
    private final FuelComponent fuelTank;

    @Intake
    public Vehicle(@Fuel("Diesel") FuelComponent tank) {
        this.fuelTank = tank;
    }

    public String getFuel() {
        return "This vehicle uses: " + fuelTank.getName();
    }
}
