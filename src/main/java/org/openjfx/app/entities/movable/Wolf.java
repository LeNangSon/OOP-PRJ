package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;

public class Wolf extends Carnivore {

    public Wolf(Vector2D position){
        super(position, 30.0, "circle", 150.0, 10.0, 1.5);
        this.setRadius(300.0);
        this.setMaxSpeed(50);
        this.type = EntityType.WOLF;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }

}
