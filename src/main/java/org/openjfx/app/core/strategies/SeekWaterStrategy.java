package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class SeekWaterStrategy implements MoveStrategy {
    private static final double STEERING_GAIN = 4.0;
    private static final double MEMORY_ARRIVE_RANGE = 24.0;
    private static final double WAYPOINT_ADVANCE_RADIUS = 10.0;
    private static final double REPLAN_TARGET_MOVE = 48.0;

    private final WanderStrategy searchWander;
    private Vector2D lastKnownWaterPos;
    private Vector2D cachedTarget;
    private List<Vector2D> cachedPath;
    private int waypointIdx;

    public SeekWaterStrategy(double wanderDistance, double wanderRadius) {
        this.searchWander = new WanderStrategy(wanderDistance, wanderRadius);
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        Vector2D currentPos = owner.getPosition();
        Vector2D nearestWater = world.findNearestTerrainPositionInRadius(
                currentPos, TerrainType.WATER, owner.getVisionRadius());

        if (nearestWater != null) {
            lastKnownWaterPos = nearestWater;
            double drinkDistance = Math.max(10.0, owner.getSize() * 0.4);
            if (currentPos.distance(nearestWater) < drinkDistance) {
                owner.setVelocity(new Vector2D(0, 0));
                owner.setAcceleration(new Vector2D(0, 0));
                owner.drink(dt);
                return;
            }

            Vector2D dir = currentPos.sub(nearestWater);
            double shoreOffset = Math.max(8.0, owner.getSize() * 0.35);
            Vector2D pathTarget = dir.magnitude() > 0.1
                    ? nearestWater.add(dir.normalize().multiply(shoreOffset))
                    : nearestWater;
            steerToward(owner, currentPos, pathTarget, dt, world);
            return;
        }

        if (lastKnownWaterPos != null) {
            if (currentPos.distance(lastKnownWaterPos) < MEMORY_ARRIVE_RANGE) {
                clearCache();
                searchWander.updateVelocity(owner, neighbors, dt, world);
            } else {
                steerToward(owner, currentPos, lastKnownWaterPos, dt, world);
            }
            return;
        }

        searchWander.updateVelocity(owner, neighbors, dt, world);
    }

    private void steerToward(LivingEntity owner, Vector2D from, Vector2D target, double dt, WorldMap world) {
        boolean needReplan = cachedPath == null
                || waypointIdx >= cachedPath.size()
                || cachedTarget == null
                || target.distance(cachedTarget) > REPLAN_TARGET_MOVE
                || owner.getBlockedLastStep();

        if (needReplan) {
            List<Vector2D> rawPath = world.findPathAStar(owner, from, target);
            cachedPath = rawPath == null ? null : world.densifyPath(rawPath, world.getCellSize() / 3.0);
            waypointIdx = 0;
            cachedTarget = target;
        }

        if (cachedPath != null) {
            while (waypointIdx < cachedPath.size() - 1
                    && from.distance(cachedPath.get(waypointIdx)) < WAYPOINT_ADVANCE_RADIUS) {
                waypointIdx++;
            }
        }

        Vector2D desiredVelocity = null;
        if (cachedPath != null && waypointIdx < cachedPath.size()) {
            desiredVelocity = from.directionTo(cachedPath.get(waypointIdx)).multiply(owner.getMaxSpeed());
        }
        if (desiredVelocity == null) {
            desiredVelocity = from.distance(target) > 0.1
                    ? from.directionTo(target).multiply(owner.getMaxSpeed())
                    : new Vector2D(0, 0);
        }

        Vector2D steering = desiredVelocity.sub(owner.getVelocity());
        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt));
        if (newVelocity.magnitude() > 1e-6) {
            newVelocity = newVelocity.normalize().multiply(owner.getMaxSpeed());
        }
        owner.setAcceleration(acceleration);
        owner.setVelocity(newVelocity);
    }

    private void clearCache() {
        lastKnownWaterPos = null;
        cachedTarget = null;
        cachedPath = null;
        waypointIdx = 0;
    }
}
