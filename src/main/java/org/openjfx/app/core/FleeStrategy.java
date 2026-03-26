package org.openjfx.app.core;

import java.util.List;

import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class FleeStrategy implements MoveStrategy {
   private double detectionRadius;
   private double comfortDistance;

   public FleeStrategy() {
   }

   public void findClosetThreat(LivingEntity owner, List<Entity> neighbors) {
   }

   public boolean isThreat(Entity entity) {
      return true;
   }

   public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt) {
      for(Entity neighbor : neighbors) {
         EntityType var7 = neighbor.getType();
      }

   }
}
