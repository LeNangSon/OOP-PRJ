package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;

public class Bear extends Carnivore {

    public Bear(Vector2D position) {
        super(
                position,
                50.0,
                "circle",
                200.0,
                1.5,
                2.0,
                28.0,  // maxSpeed (giảm 38→28)
                40.0,  // maxForce
                12.0,  // mass
                70.0,  // wanderDistance
                35.0   // wanderRadius
        );

        this.setVisionRadius(160.0);
        this.type = EntityType.BEAR;
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/Bear.png";
    }
}
