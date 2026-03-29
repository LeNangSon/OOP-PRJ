package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.core.strategies.SeekWaterStrategy;
import org.openjfx.app.core.strategies.WanderStrategy;



public abstract class Herbivore extends LivingEntity {

    public Herbivore(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape, initialHealth, hungerRate, thirstRate);

    }
    @Override
    public void eat(){
        
    };
    @Override
    public void drink(){};
    @Override
    public void update(double dt, WorldMap world){
        this.neighbors = world.getNeighbors(this, this.radius);

        if (hasThreat(this, neighbors)){
            if (!(this.moveStrategy instanceof FleeStrategy)) {
                this.moveStrategy = new FleeStrategy();
            }
        }else if(this.getThirst() > 70.0){
            if (!(this.moveStrategy instanceof SeekWaterStrategy)){
                this.moveStrategy = new SeekWaterStrategy(this.wanderSpeed, this.wanderR);
            }
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
