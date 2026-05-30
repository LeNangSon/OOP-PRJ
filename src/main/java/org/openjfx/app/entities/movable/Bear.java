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
                38.0,
                30.0,
                12.0,
                70.0,
                35.0
        );

        this.setVisionRadius(200.0);
        this.type = EntityType.BEAR;
        this.setVelocity(new Vector2D(8.0, 0.0));
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
