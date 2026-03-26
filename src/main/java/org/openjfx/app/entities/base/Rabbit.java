package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;

public class Rabbit extends LivingEntity {
    


    //Constructor
    public Rabbit (Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape, initialHealth, hungerRate, thirstRate);
        this.type = EntityType.RABBIT;

    }
    
}
