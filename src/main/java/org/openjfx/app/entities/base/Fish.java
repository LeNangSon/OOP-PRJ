package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public class Fish extends Animal {
    
    public Fish(Vector2D position){
        super(position, 15.0, "ellipse", 100.0, 0.5, 0.0);
        this.setRadius(30.0);
        this.type = EntityType.FISH;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    
}
