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
    private static final double DRINK_RANGE   = 20.0;
    private static final double MEMORY_ARRIVE_RANGE = 24.0;
    private static final double WAYPOINT_ADVANCE_RADIUS = 10.0;
    private static final double REPLAN_TARGET_MOVE = 48.0;

    private Vector2D lastKnownWaterPos = null;
    private List<Vector2D> cachedPath = null;
    private int waypointIdx = 0;
    private Vector2D cachedTarget = null;

    public SeekWaterStrategy(double wanderSpeed, double wanderR) {
        this.searchWander = new WanderStrategy(wanderSpeed, wanderR);
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        Vector2D currentPos = owner.getPosition();

        // Tìm nguồn nước trong tầm nhìn
        Vector2D nearestWater = world.findNearestTerrainPositionInRadius(
                currentPos, TerrainType.WATER, owner.getVisionRadius());

        if (nearestWater != null) {
            // Cập nhật bộ nhớ khi thấy nước
            lastKnownWaterPos = nearestWater;

            if (currentPos.distance(nearestWater) < DRINK_RANGE) {
                // Đang ở cạnh nước → uống
                owner.setVelocity(new Vector2D(0, 0));
                owner.drink(dt);
                return;
            }
            // Di chuyển đến nguồn nước tìm thấy
            steerToward(owner, currentPos, nearestWater, dt, world);
            return;
        }

        // Không thấy nước trong tầm nhìn → dùng bộ nhớ
        if (lastKnownWaterPos != null) {
            double distToMemory = currentPos.distance(lastKnownWaterPos);
            if (distToMemory < MEMORY_ARRIVE_RANGE) {
                // Đã đến nơi nhớ nhưng không có nước → xóa bộ nhớ và cache, chuyển sang wander
                lastKnownWaterPos = null;
                cachedPath = null;
                searchWander.updateVelocity(owner, neighbors, dt, world);
            } else {
                // Đi về hướng nguồn nước đã nhớ
                steerToward(owner, currentPos, lastKnownWaterPos, dt, world);
            }
            return;
        }

        // Không có bộ nhớ → đi lang thang tìm kiếm
        searchWander.updateVelocity(owner, neighbors, dt, world);
    }

    private void steerToward(LivingEntity owner, Vector2D from, Vector2D target, double dt, WorldMap world) {
        boolean needReplan = cachedPath == null
                || waypointIdx >= cachedPath.size()
                || cachedTarget == null
                || target.distance(cachedTarget) > REPLAN_TARGET_MOVE;

        if (needReplan) {
            List<Vector2D> rawPath = world.findPathAStar(owner, from, target);
            if (rawPath != null && !rawPath.isEmpty()) {
                cachedPath = world.densifyPath(rawPath, world.getCellSize() / 3.0);
            } else {
                cachedPath = null;
            }
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
            double dist = from.distance(target);
            desiredVelocity = dist > 0.1
                    ? from.directionTo(target).multiply(owner.getMaxSpeed())
                    : new Vector2D(0, 0);
        }

        Vector2D steering = desiredVelocity.sub(owner.getVelocity());
        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        owner.setAcceleration(acceleration);
        owner.setVelocity(owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed()));
    }
}
