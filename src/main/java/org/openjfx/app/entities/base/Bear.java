package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public class Bear extends LivingEntity{
    

    public Bear(Vector2D position){
        super(position, 80.0, "Rect", 100.0, 2.5, 3.0);
        this.setRadius(80.0);
        this.type = EntityType.BEAR;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    
}