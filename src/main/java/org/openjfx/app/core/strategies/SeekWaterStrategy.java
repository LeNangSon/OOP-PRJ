package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class SeekWaterStrategy implements MoveStrategy {
    private WanderStrategy searchWander;

    public SeekWaterStrategy(double wanderSpeed, double wanderR) {
        this.searchWander = new WanderStrategy(wanderSpeed, wanderR);
    }
    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world){
        Vector2D currentPos = owner.getPosition();

        if (world.getTerrainAt(currentPos) == TerrainType.WATER) {
            owner.setVelocity(new Vector2D(0, 0));
            owner.drink(dt);
            return;
        }

        Vector2D nearestWater = world.findNearestTerrainPosition(currentPos, TerrainType.WATER);
        if (nearestWater == null) {
            this.searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        double distanceToWater = currentPos.distance(nearestWater);
        if(distanceToWater > 5){
            Vector2D direct = currentPos.directionTo(nearestWater);
            owner.setVelocity(direct.multiply(owner.getMaxSpeed()));
        } else {
            owner.setVelocity(new Vector2D(0, 0));
        }
    }
}
