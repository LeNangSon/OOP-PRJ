package org.openjfx.app.core.strategies;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainGrid;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class HunterStrategy implements MoveStrategy {

    private static final double STEERING_GAIN = 4.0;
    private static final int BLOCKED_WAYPOINTS_TO_AVOID = 2;
    private double logCooldown = 0;

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
            int targetId = findClosestPrey(owner, neighbors, world);

            if (targetId != -1) {
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

                    if (range < world.getInteractionDistance(owner, prey)) {
                        owner.setAcceleration(new Vector2D(0, 0));
                        owner.setVelocity(new Vector2D(0, 0));
                        
                        // --- SỬA TÊN#ID Ở ĐÂY ---
                        world.notifyAction(ownerName, "đã bắt được", preyName);
                        
                        owner.eat(prey, dt);
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
                        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt));
                        if (newVelocity.magnitude() > 1e-6) {
                            newVelocity = newVelocity.normalize().multiply(owner.getMaxSpeed());
                        }
                        owner.setAcceleration(acceleration);
                        owner.setVelocity(newVelocity);
                    }
                }
            } else {
                logCooldown = 0;
                clearDebugPathState(owner.getId());

            }
        }
    }

    private Set<String> collectBlockedWaypointKeys(int ownerId, WorldMap world) {
        DebugPathState lastPathState = DEBUG_PATH_STATES.get(ownerId);
        if (lastPathState == null || lastPathState.getPath() == null || lastPathState.getPath().isEmpty()) {
            return null;
        }

        Set<String> blockedKeys = new HashSet<>();
        List<Vector2D> lastPath = lastPathState.getPath();
        int limit = Math.min(BLOCKED_WAYPOINTS_TO_AVOID, lastPath.size());
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
