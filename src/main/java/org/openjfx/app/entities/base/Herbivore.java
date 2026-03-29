package org.openjfx.app.entities.base;

import org.openjfx.app.core.FleeStrategy;
import org.openjfx.app.core.HunterStrategy;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WanderStrategy;
import org.openjfx.app.core.WorldMap;



public abstract class Herbivore extends LivingEntity {

    public Herbivore(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape, initialHealth, hungerRate, thirstRate);

    }

    @Override
    public void update(double dt, WorldMap world){  

        this.neighbors = world.getNeighbors(this, this.radius);

        if (hasThreat(this, neighbors)){
            if (!(this.moveStrategy instanceof FleeStrategy)) {
                this.moveStrategy = new FleeStrategy();
            }
        }else if(this.getThirst() > 70.0){
            /*if (!(this.moveStrategy instanceof ThirstStrategy)){
                this.moveStrategy = new ThirstStrategy();
            }*/
        }else if (this.getHunger() > 70.0){
            if (!(this.moveStrategy instanceof HunterStrategy)){
                this.moveStrategy = new HunterStrategy();
            }
        }else {
            if (!(this.moveStrategy instanceof WanderStrategy)) {
                this.moveStrategy = new WanderStrategy(this.wanderSpeed, this.wanderR);
            }
        }

        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, neighbors, dt, world);
        }

        super.update(dt, world);

    }


    
}
