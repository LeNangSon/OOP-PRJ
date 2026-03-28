package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public class Wolf extends LivingEntity{
    

    public Wolf(Vector2D position){
        super(position, 60.0, "triangle", 100.0, 3.5, 4.5);
        this.setRadius(100.0);
        this.type = EntityType.WOLF;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    
}