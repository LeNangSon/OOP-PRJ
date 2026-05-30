package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Elephant extends Herbivore {

    public Elephant(Vector2D position) {
        super(
                position,
                50.0,
                "rect",
                100.0,
                5.0,
                6.0,
                26.0,
                36.0,
                10.0,
                100.0,
                40.0
        );

        this.setVisionRadius(150.0);
        this.setThirst(80.0);
        this.type = EntityType.ELEPHANT;
        this.setVelocity(new Vector2D(8.0, 0.0));
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/Elephant.png";
    }
}
