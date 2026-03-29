package org.openjfx.app.core;

import java.util.List;

import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;



public class FleeStrategy implements MoveStrategy {

   public FleeStrategy() {
   }

   public int findClosetThreat(LivingEntity owner, List<Entity> neighbors) {
    double minDistance=99999999f;
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
                // Tìm vecto(BA) rồi chuẩn hóa
                Vector2D directionShouldRun = threat.getPosition().directionTo(owner.getPosition());
                owner.setVelocity(directionShouldRun.multiply(owner.getMaxSpeed()));

            }
        } else {
            owner.setVelocity(new Vector2D(0, 0));
            
        }
        
        

    }


      

   }
}
