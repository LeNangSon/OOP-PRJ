package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Wolf extends Herbivore {

    public Wolf(Vector2D position) {
        super(position, 40.0, "circle", 100.0, 8.0, 7.0);
        this.type = EntityType.WOLF;
        this.setRadius(20.0);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Kế thừa logic săn mồi, đuổi theo Rabbit/Elephant từ Canivore
        super.update(dt, world);
    }
}