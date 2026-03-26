package org.openjfx.app.core;

import java.util.List;

import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;


public class FleeStrategy implements MoveStrategy {
    private double detectionRadius;
    private double comfortDistance;


    public void findClosetThreat(LivingEntity owner, List<Entity> neighbors){

    }

    public boolean isThreat(Entity entity){
        return true;

    }


    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt){
        //Duyệt từng vật thể một,
        for (Entity neighbor: neighbors){
            EntityType cur = neighbor.getType();
            
        }           
            
        

    }
    
}
