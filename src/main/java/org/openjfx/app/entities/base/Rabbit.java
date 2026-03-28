package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public class Rabbit extends Herbivore{
    

    public Rabbit(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, 20.0, "circle", 100.0, 1.0, 1.5);
        this.setRadius(40.0);
        this.type = EntityType.RABBIT;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    
}
