package org.openjfx.app.core.strategies;

import java.util.ArrayList;
import java.util.List;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class SeekWaterStrategy implements MoveStrategy {
    private final WanderStrategy searchWander;
    private static final double STEERING_GAIN = 4.0;

    // Stuck detection
    private static final double STUCK_CHECK_INTERVAL = 1.0;
    private static final double STUCK_DIST_THRESHOLD  = 8.0;
    private static final double STUCK_TIME_LIMIT      = 6.0;

    // Blacklist
    private static final double BLACKLIST_RADIUS = 50.0;

    // No-target → forced wander
    private static final double NO_TARGET_TIMEOUT      = 3.0;
    private static final double FORCED_WANDER_DURATION = 2.0;

    private Vector2D lastCheckedPos = null;
    private double   checkTimer     = 0.0;
    private double   stuckTimer     = 0.0;

    private Vector2D           currentTarget     = null;
    private final List<Vector2D> blacklistedWaters = new ArrayList<>();

    private double noTargetTimer     = 0.0;
    private double forcedWanderTimer = 0.0;

    public SeekWaterStrategy(double wanderSpeed, double wanderR) {
        this.searchWander = new WanderStrategy(wanderSpeed, wanderR);
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        Vector2D currentPos = owner.getPosition();

        // ── Forced wander: lang thang 2s rồi mới tìm lại ────────────────────
        if (forcedWanderTimer > 0) {
            forcedWanderTimer -= dt;
            searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        // ── Stuck detection ──────────────────────────────────────────────────
        checkTimer += dt;
        if (checkTimer >= STUCK_CHECK_INTERVAL) {
            checkTimer = 0.0;
            if (lastCheckedPos != null && currentTarget != null) {
                if (currentPos.distance(lastCheckedPos) < STUCK_DIST_THRESHOLD) {
                    stuckTimer += STUCK_CHECK_INTERVAL;
                } else {
                    stuckTimer = 0.0;
                }
            }
            lastCheckedPos = currentPos;
        }

        if (stuckTimer >= STUCK_TIME_LIMIT && currentTarget != null) {
            blacklistedWaters.add(currentTarget);
            currentTarget = null;
            stuckTimer    = 0.0;
        }

        // ── Chọn mục tiêu nước ───────────────────────────────────────────────
        if (currentTarget == null || isBlacklisted(currentTarget)) {
            currentTarget = findBestWater(owner, currentPos, world);
        }

        // Không tìm thấy nước → tích lũy timer, sau 3s thì forced wander 2s
        if (currentTarget == null) {
            noTargetTimer += dt;
            if (noTargetTimer >= NO_TARGET_TIMEOUT) {
                forcedWanderTimer = FORCED_WANDER_DURATION;
                noTargetTimer     = 0.0;
            }
            searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        // Tìm thấy nước → reset no-target timer
        noTargetTimer = 0.0;

        // Đến đủ gần → uống nước
        if (currentPos.distance(currentTarget) < 20) {
            owner.setVelocity(new Vector2D(0, 0));
            owner.drink(dt);
            stuckTimer    = 0.0;
            currentTarget = null;
            return;
        }

        // ── Tìm đường A* ─────────────────────────────────────────────────────
        List<Vector2D> path = world.findPathAStar(owner, currentPos, currentTarget);

        if (path == null || path.isEmpty()) {
            blacklistedWaters.add(currentTarget);
            currentTarget = null;
            stuckTimer    = 0.0;
            searchWander.updateVelocity(owner, neighbors, dt, world);
            return;
        }

        // ── Đi theo đường ────────────────────────────────────────────────────
        Vector2D desiredVelocity = null;
        for (Vector2D waypoint : path) {
            if (waypoint != null && currentPos.distance(waypoint) > 1) {
                desiredVelocity = currentPos.directionTo(waypoint).multiply(owner.getMaxSpeed());
                break;
            }
        }

        if (desiredVelocity == null) {
            double dist = currentPos.distance(currentTarget);
            desiredVelocity = dist > 0.1
                    ? currentPos.directionTo(currentTarget).multiply(owner.getMaxSpeed())
                    : new Vector2D(0, 0);
        }

        Vector2D steering     = desiredVelocity.sub(owner.getVelocity());
        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        Vector2D newVelocity  = owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed());
        owner.setAcceleration(acceleration);
        owner.setVelocity(newVelocity);
    }

    private Vector2D findBestWater(LivingEntity owner, Vector2D currentPos, WorldMap world) {
        Vector2D found = world.findNearestTerrainPositionExcluding(
                currentPos, TerrainType.WATER, owner.getVisionRadius(),
                blacklistedWaters, BLACKLIST_RADIUS);
        if (found != null) return found;
        return world.findNearestTerrainPositionExcluding(
                currentPos, TerrainType.WATER, -1,
                blacklistedWaters, BLACKLIST_RADIUS);
    }

    private boolean isBlacklisted(Vector2D pos) {
        for (Vector2D bl : blacklistedWaters) {
            if (pos.distance(bl) < BLACKLIST_RADIUS) return true;
        }
        return false;
    }
}
