package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class SeekWaterStrategy implements MoveStrategy {
    private WanderStrategy searchWander;
    private static final double STEERING_GAIN = 4.0;
    public SeekWaterStrategy(double wanderSpeed, double wanderR) {
        this.searchWander = new WanderStrategy(wanderSpeed, wanderR);
    }
    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world){
        Vector2D currentPos = owner.getPosition();



        // Tìm nguồn nước từ vị trí hiện tại
        Vector2D nearestWater = world.findNearestTerrainPositionInRadius(currentPos, TerrainType.WATER,owner.getVisionRadius());

        // Nếu không tìm thấy thì đi lang thang
        if (nearestWater == null) {
            this.searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        // Nếu vị trí đang đứng là nước --> Uống
        if (currentPos.distance(nearestWater) < 20) {
            owner.setVelocity(new Vector2D(0, 0));
            owner.drink(dt);
            return;
        }
        List<Vector2D> path = world.findPathAStar(owner, currentPos, nearestWater);
        Vector2D desiredVelocity = null;
        if (path != null && !path.isEmpty()) {
            Vector2D nextWaypoint = null;
            for (Vector2D waypoint : path) {
                if (waypoint != null && currentPos.distance(waypoint) > 1) {
                    nextWaypoint = waypoint;
                    break;
                }
            }
            if (nextWaypoint != null) {
                desiredVelocity = currentPos.directionTo(nextWaypoint).multiply(owner.getMaxSpeed());
            }
        }

        if (desiredVelocity == null) {
            double distToWater = currentPos.distance(nearestWater);
            if (distToWater > 0.1) {
                desiredVelocity = currentPos.directionTo(nearestWater).multiply(owner.getMaxSpeed());
            } else {
                desiredVelocity = new Vector2D(0, 0);
            }
        }

        // Fallback: direct steering if no path found
        Vector2D steering = desiredVelocity.sub(owner.getVelocity());
        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed());
        owner.setAcceleration(acceleration);
        owner.setVelocity(newVelocity);
    }
}
