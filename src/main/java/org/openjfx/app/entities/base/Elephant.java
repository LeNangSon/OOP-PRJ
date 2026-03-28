package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public class Elephant extends LivingEntity{

    public Elephant(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, 120.0, "rect", 100.0, 5.0, 6.0);
        this.setRadius(70.0);
        this.type = EntityType.ELEPHANT;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    
}