package org.openjfx.app.core.strategies;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class SeekWaterStrategy implements MoveStrategy {
    private List<Entity> knownWaters = new ArrayList<>();

    private WanderStrategy searchWander;

    public SeekWaterStrategy(double wanderSpeed, double wanderR) {
        this.searchWander = new WanderStrategy(wanderSpeed, wanderR);
    }

    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world){
        for(Entity e : neighbors){
            if (e.getType() == EntityType.WATER && !this.knownWaters.contains(e)) {
                this.knownWaters.add(e);
            }
        }

        if(this.knownWaters.isEmpty()) {
            this.searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        Vector2D currentPos = owner.getPosition();
        Entity closestWater = null;
        double min_distance = Double.MAX_VALUE;

        for(Entity water: this.knownWaters){
                double distance = currentPos.distance(water.getPosition());
                if(distance < min_distance) {
                    closestWater = water;
                    min_distance = distance;
                }
        }

        Vector2D targetPos = closestWater.getPosition();

        if(min_distance > 5){                                           // <=5 là uống nước
            Vector2D direct = currentPos.directionTo(targetPos);
            owner.setVelocity(direct.multiply(owner.getMaxSpeed()));
        }
        else{
            owner.setVelocity(new Vector2D(0,0));
            owner.drink();
        }
    }
}
