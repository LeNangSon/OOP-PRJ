package org.openjfx.app.entities.base;

import org.openjfx.app.core.MoveStrategy;
import org.openjfx.app.core.Vector2D;



public class Herbivore extends LivingEntity {
    private MoveStrategy moveForHerbivore ;

    public Herbivore(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape, initialHealth, hungerRate, thirstRate);

    }


    @Override
    public void eat(){};
    @Override
    public void drink(){};


    
}
