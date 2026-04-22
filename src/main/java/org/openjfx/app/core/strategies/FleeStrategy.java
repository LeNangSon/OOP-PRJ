package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;



public class FleeStrategy implements MoveStrategy {
    private static final double STEERING_GAIN = 4.0;

   public FleeStrategy() {
   }

   public int findClosetThreat(LivingEntity owner, List<Entity> neighbors) {
    double minDistance= Double.MAX_VALUE;
    int closiestID = -1;
    for(Entity neighbor : neighbors) {
        EntityType curNeighborType = neighbor.getType();
        if (RelationManager.isScaredOf(owner.getType(), curNeighborType)){
            Vector2D ownerPosition = owner.getPosition();
            double distance = ownerPosition.distance(neighbor.getPosition());
            if (distance <= minDistance){
                minDistance = distance;
                closiestID = neighbor.getId();
            }
        }

      }
      return closiestID;

   }
   @Override
   public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
    if(owner.isAlive()){
        int mostDangerous = findClosetThreat(owner, neighbors);

        if (mostDangerous != -1){
            Entity threat = world.getEntityById(mostDangerous);
            if (threat != null) {
                Vector2D desiredVelocity = threat.getPosition().directionTo(owner.getPosition()).multiply(owner.getMaxSpeed());
                Vector2D steering = desiredVelocity.sub(owner.getVelocity());
                Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
                Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed());
                owner.setAcceleration(acceleration);
                owner.setVelocity(newVelocity);

                

            }
        } else {
            
            
        }
        
        

    }


      

   }




   


      

   
}
