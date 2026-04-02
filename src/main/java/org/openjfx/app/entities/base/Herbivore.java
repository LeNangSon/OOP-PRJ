package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.core.strategies.SeekWaterStrategy;
import org.openjfx.app.core.strategies.WanderStrategy;
import org.openjfx.app.entities.staticobjs.Plant;



public abstract class Herbivore extends LivingEntity {

    public Herbivore(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape, initialHealth, hungerRate, thirstRate);
    }
    @Override
    public void eat(Entity target, double dt){
            setHunger(this.getHunger() - ((Plant)target).consume());
    };

    @Override
    public void update(double dt, WorldMap world){
        System.out.println(this.getThirst());
        this.neighbors = world.getNeighbors(this, this.radius);

        boolean isCurrentlySeekingWater = (this.moveStrategy instanceof SeekWaterStrategy)
                && (this.getThirst() > this.getThirstRate() * dt);

        boolean isCurrentlyHunting = (this.moveStrategy instanceof HunterStrategy)
                && (this.getHunger() > this.getHungerRate() * dt);

        if (hasThreat(this, neighbors)){
            if (!(this.moveStrategy instanceof FleeStrategy)) {
                this.moveStrategy = new FleeStrategy();
            }
        }else if(isCurrentlySeekingWater || this.getThirst() > 70.0){
            if (!(this.moveStrategy instanceof SeekWaterStrategy)){
                this.moveStrategy = new SeekWaterStrategy(this.wanderSpeed, this.wanderR);
            }
        }else if (isCurrentlyHunting || this.getHunger() > 70.0){
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
