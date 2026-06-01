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



        // Tìm mép nước gần nhất
        Vector2D nearestWater = world.findNearestTerrainPositionInRadius(currentPos, TerrainType.WATER, owner.getVisionRadius());

        if (nearestWater == null) {
            this.searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        double drinkDistance = Math.max(10.0, owner.getSize() * 0.4);

        // Chỉ uống khi đã đứng sát mép nước.
        if (currentPos.distance(nearestWater) < drinkDistance) {
            owner.setVelocity(new Vector2D(0, 0));
            owner.drink(dt);
            return;
        }

        // Pathfind đến điểm sát mép nước nhưng vẫn đứng được trên đất.
        // (tránh A* bị reject vì target cell nằm trong vùng nước)
        Vector2D dir = currentPos.sub(nearestWater);
        double shoreOffset = Math.max(8.0, owner.getSize() * 0.35);
        Vector2D pathTarget = dir.magnitude() > 0.1
                ? nearestWater.add(dir.normalize().multiply(shoreOffset))
                : nearestWater;

        List<Vector2D> path = world.findPathAStar(owner, currentPos, pathTarget);
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
        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt));
        if (newVelocity.magnitude() > 1e-6) {
            newVelocity = newVelocity.normalize().multiply(owner.getMaxSpeed());
        }
        owner.setAcceleration(acceleration);
        owner.setVelocity(newVelocity);
    }
}
