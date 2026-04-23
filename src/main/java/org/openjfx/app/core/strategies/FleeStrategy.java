package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class FleeStrategy implements MoveStrategy {
    private static final double STEERING_GAIN = 4.0;
    private static final double DEFAULT_WANDER_DISTANCE_FACTOR = 0.6;
    private static final double DEFAULT_WANDER_RADIUS_FACTOR = 0.35;

    // --- PHẦN THÊM: Biến kiểm soát tần suất log ---
    private double logCooldown = 0;

    public FleeStrategy() {
    }


    public int findClosetThreat(LivingEntity owner, List<Entity> neighbors) {
        double minDistance = Double.MAX_VALUE;
        int closiestID = -1;
        for (Entity neighbor : neighbors) {
            EntityType curNeighborType = neighbor.getType();
            if (RelationManager.isScaredOf(owner.getType(), curNeighborType)) {
                Vector2D ownerPosition = owner.getPosition();
                double distance = ownerPosition.distance(neighbor.getPosition());
                if (distance <= minDistance) {
                    minDistance = distance;
                    closiestID = neighbor.getId();
                }
            }
        }
        return closiestID;
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (!owner.isAlive()) {
            return;
        }

        int mostDangerous = findClosetThreat(owner, neighbors);
        if (mostDangerous == -1) {
            // Reset log khi an toàn
            logCooldown = 0;

            double baseRadius = Math.max(owner.getRadius(), 10.0);
            double wanderDistance = baseRadius * DEFAULT_WANDER_DISTANCE_FACTOR;
            double wanderRadius = baseRadius * DEFAULT_WANDER_RADIUS_FACTOR;
            WanderStrategy wanderFallback = new WanderStrategy(wanderDistance, wanderRadius);
            wanderFallback.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        Entity threat = world.getEntityById(mostDangerous);
        if (threat == null) {
            logCooldown = 0;
            double baseRadius = Math.max(owner.getRadius(), 10.0);
            double wanderDistance = baseRadius * DEFAULT_WANDER_DISTANCE_FACTOR;
            double wanderRadius = baseRadius * DEFAULT_WANDER_RADIUS_FACTOR;
            WanderStrategy wanderFallback = new WanderStrategy(wanderDistance, wanderRadius);
            wanderFallback.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        // --- PHẦN THÊM: Gửi thông báo chạy trốn ---
        logCooldown -= dt;
        if (logCooldown <= 0) {
            world.notifyAction(
                owner.getType().toString(), 
                "đang chạy trốn khỏi", 
                threat.getType().toString()
            );
            logCooldown = 2.5; // 2.5 giây sau mới hiện lại
        }
        // ----------------------------------------

        Vector2D ownerPos = owner.getPosition();
        Vector2D desiredVelocity = null;

        // Prefer a path-driven flee direction first.
        Vector2D destination = world.findNearestTerrainPositionInRadius(ownerPos, TerrainType.BUSH, owner.getRadius());
        if (destination == null) {
            destination = world.findNearestTerrainPositionInRadius(ownerPos, TerrainType.LAND, owner.getRadius());
        }

        if (destination != null) {
            List<Vector2D> path = world.findPathAStar(owner, ownerPos, destination);
            if (path != null && !path.isEmpty()) {
                Vector2D nextWaypoint = null;
                for (Vector2D waypoint : path) {
                    if (waypoint != null && ownerPos.distance(waypoint) > 1e-3) {
                        nextWaypoint = waypoint;
                        break;
                    }
                }
                if (nextWaypoint != null) {
                    desiredVelocity = ownerPos.directionTo(nextWaypoint).multiply(owner.getMaxSpeed());
                }
            }
        }

        // Fallback: steer directly away from threat.
        if (desiredVelocity == null) {
            desiredVelocity = threat.getPosition().directionTo(ownerPos).multiply(owner.getMaxSpeed());
        }

        Vector2D steering = desiredVelocity.sub(owner.getVelocity());
        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed());
        owner.setAcceleration(acceleration);
        owner.setVelocity(newVelocity);
        double distance = owner.getPosition().distance(threat.getPosition());
        if (distance < 5) {
            owner.setHealth(owner.getHealth() - 10*dt);
        }
    }

    
}