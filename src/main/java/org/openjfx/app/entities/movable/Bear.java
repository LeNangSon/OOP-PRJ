package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Bear extends Herbivore {

    public Bear(Vector2D position) {
        super(position, 100.0, "rect", 300.0, 5.0, 5.0);
        this.type = EntityType.BEAR;
        this.setRadius(50.0); // Bán kính va chạm lớn
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
    }
}