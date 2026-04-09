package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;

public class Bear extends Carnivore {

    public Bear(Vector2D position){
        super(position, 20.0, "circle", 100.0, 1.0, 1.5);
        this.setRadius(40.0);
        this.type = EntityType.BEAR;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }

}
