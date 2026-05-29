package org.openjfx.app.core.strategies;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openjfx.app.core.DeathCause;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainGrid;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class HunterStrategy implements MoveStrategy {

    private static final double STEERING_GAIN = 4.0;
    private static final int MIN_BLOCKED_WAYPOINTS = 3;
    private static final int MAX_BLOCKED_WAYPOINTS = 20;
    private static final double DEFAULT_WANDER_DISTANCE_FACTOR = 0.6;
    private static final double DEFAULT_WANDER_RADIUS_FACTOR = 0.35;
    private static final double NO_TARGET_TIMEOUT      = 3.0;
    private static final double FORCED_WANDER_DURATION = 2.0;

    private double logCooldown      = 0;
    private double noTargetTimer    = 0.0;
    private double forcedWanderTimer = 0.0;
    private WanderStrategy wanderFallback;

    public static final class DebugPathState {
        private final List<Vector2D> path;
        public DebugPathState(List<Vector2D> path) { this.path = path; }
        public List<Vector2D> getPath() { return path; }
    }

    private static final Map<Integer, DebugPathState> DEBUG_PATH_STATES = new ConcurrentHashMap<>(); 

    public HunterStrategy() {}

    public static DebugPathState getDebugPathState(int entityId) {
        return DEBUG_PATH_STATES.get(entityId);
    }

    public static void clearDebugPathState(int entityId) {
        DEBUG_PATH_STATES.remove(entityId);
    }

    public int findClosestPrey(LivingEntity owner, List<Entity> neighbors, WorldMap world) {
        double minDistance = Double.MAX_VALUE;
        int closestID = -1;
        for (Entity neighbor : neighbors) {
            // Bỏ qua cây đã bị ăn (consume) trong cùng frame nhưng chưa kịp remove khỏi entities.
            if (neighbor instanceof org.openjfx.app.entities.staticobjs.Plant
                    && !((org.openjfx.app.entities.staticobjs.Plant) neighbor).isAlive()) {
                continue;
            }
            if (RelationManager.isPrey(neighbor.getType(), owner.getType()) && world.getTerrainAt(neighbor.getPosition()) != TerrainType.BUSH) {
                double distance = owner.getPosition().distance(neighbor.getPosition());
                if (distance < minDistance) {
                    minDistance = distance;
                    closestID = neighbor.getId();
                }
            }
        }
        return closestID;
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (owner.isAlive()) {
            // Forced wander: lang thang 2s rồi mới săn lại
            if (forcedWanderTimer > 0) {
                forcedWanderTimer -= dt;
                runWanderFallback(owner, neighbors, dt, world);
                return;
            }

            int targetId = findClosestPrey(owner, neighbors, world);

            if (targetId != -1) {
                noTargetTimer = 0.0;
                Entity prey = world.getEntityById(targetId);
                if (prey != null) {
                    // --- SỬA TÊN#ID Ở ĐÂY ---
                    String ownerName = owner.getClass().getSimpleName() + "#" + owner.getId();
                    String preyName = prey.getClass().getSimpleName() + "#" + prey.getId();

                    logCooldown -= dt;
                    if (logCooldown <= 0) {
                        world.notifyAction(ownerName, "đang săn đuổi", preyName);
                        logCooldown = 3.0; 
                    }

                    double range = owner.getPosition().distance(prey.getPosition());

                    if (range < 2) {
                        owner.setAcceleration(new Vector2D(0, 0));
                        owner.setVelocity(new Vector2D(0, 0));

                        // --- SỬA TÊN#ID Ở ĐÂY ---
                        world.notifyAction(ownerName, "đã bắt được", preyName);

                        boolean preyWasAlive = prey instanceof LivingEntity
                                && ((LivingEntity) prey).isAlive();
                        owner.eat(prey, dt);
                        if (preyWasAlive && prey instanceof LivingEntity preyLiving
                                && !preyLiving.isAlive()) {
                            world.recordDeath(prey.getType(), DeathCause.PREDATION);
                            world.broadcastDeath(preyName + " đã chết vì bị " + ownerName + " săn");
                        }
                    } else {
                        Vector2D ownerPos = owner.getPosition();
                        Vector2D preyPos = prey.getPosition();
                        Set<String> avoidedGridKeys = null;
                        if (owner.getBlockedLastStep()) {
                            avoidedGridKeys = collectBlockedWaypointKeys(owner.getId(), world);
                        }

                        List<Vector2D> path = world.findPathAStar(owner, ownerPos, preyPos, avoidedGridKeys);

                        Vector2D desiredVelocity = null;
                        if (path != null && !path.isEmpty()) {
                            DEBUG_PATH_STATES.put(owner.getId(), new DebugPathState(path));
                            Vector2D nextWaypoint = null;
                            for (Vector2D waypoint : path) {
                                if (waypoint != null && ownerPos.distance(waypoint) > 1) {
                                    nextWaypoint = waypoint;
                                    break;
                                }
                            }
                            if (nextWaypoint != null) {
                                desiredVelocity = ownerPos.directionTo(nextWaypoint).multiply(owner.getMaxSpeed());
                            }
                        } else {
                            clearDebugPathState(owner.getId());
                        }

                        if (desiredVelocity == null) {
                            desiredVelocity = ownerPos.directionTo(preyPos).multiply(owner.getMaxSpeed());
                        }

                        Vector2D steering = desiredVelocity.sub(owner.getVelocity());
                        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
                        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed());
                        owner.setAcceleration(acceleration);
                        owner.setVelocity(newVelocity);
                    }
                }
            } else {
                // Không tìm thấy mồi
                logCooldown = 0;
                clearDebugPathState(owner.getId());

                noTargetTimer += dt;
                if (noTargetTimer >= NO_TARGET_TIMEOUT) {
                    forcedWanderTimer = FORCED_WANDER_DURATION;
                    noTargetTimer     = 0.0;
                }
                runWanderFallback(owner, neighbors, dt, world);
            }
        }
    }

    private void runWanderFallback(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (wanderFallback == null) {
            double baseRadius = Math.max(owner.getVisionRadius(), 10.0);
            wanderFallback = new WanderStrategy(
                    baseRadius * DEFAULT_WANDER_DISTANCE_FACTOR,
                    baseRadius * DEFAULT_WANDER_RADIUS_FACTOR);
        }
        wanderFallback.updateVelocity(owner, neighbors, dt, world);
    }

    private Set<String> collectBlockedWaypointKeys(int ownerId, WorldMap world) {
        DebugPathState lastPathState = DEBUG_PATH_STATES.get(ownerId);
        if (lastPathState == null || lastPathState.getPath() == null || lastPathState.getPath().isEmpty()) {
            return null;
        }

        Set<String> blockedKeys = new HashSet<>();
        List<Vector2D> lastPath = lastPathState.getPath();
        int halfPlusOne = (lastPath.size() / 2) + 1;
        int limit = Math.min(lastPath.size(),
                    Math.min(MAX_BLOCKED_WAYPOINTS,
                    Math.max(MIN_BLOCKED_WAYPOINTS, halfPlusOne)));
        for (int i = 0; i < limit; i++) {
            Vector2D point = lastPath.get(i);
            if (point == null) continue;
            TerrainGrid.GridCoordinate grid = world.worldToGrid(point);
            if (grid != null) {
                blockedKeys.add(grid.getRow() + ":" + grid.getCol());
            }
        }
        return blockedKeys.isEmpty() ? null : blockedKeys;
    }
}