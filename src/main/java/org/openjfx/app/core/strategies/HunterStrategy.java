package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class HunterStrategy implements MoveStrategy {

    private static final double STEERING_GAIN = 4.0;
    // --- PHẦN THÊM: Biến kiểm soát tần suất log ---
    private double logCooldown = 0;

    public static final class DebugPathState {
        private final List<Vector2D> path;

        public DebugPathState(List<Vector2D> path) {
            this.path = path;
        }

        public List<Vector2D> getPath() {
            return path;
        }
    }

    private static final Map<Integer, DebugPathState> DEBUG_PATH_STATES = new ConcurrentHashMap<>(); 

    public HunterStrategy() {
    }

    public static DebugPathState getDebugPathState(int entityId) {
        return DEBUG_PATH_STATES.get(entityId);
    }

    public static void clearDebugPathState(int entityId) {
        DEBUG_PATH_STATES.remove(entityId);
    }

    /**
     * Tìm con mồi gần nhất trong danh sách hàng xóm
     */
    public int findClosestPrey(LivingEntity owner, List<Entity> neighbors, WorldMap world) {
        double minDistance = Double.MAX_VALUE;
        int closestID = -1;

        for (Entity neighbor : neighbors) {
            // Kiểm tra xem hàng xóm này có phải là con mồi của chủ thể không
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
                    // --- PHẦN THÊM: Bắn tin nhắn săn đuổi ra Terminal ---
                    logCooldown -= dt;
                    if (logCooldown <= 0) {
                        world.notifyAction(
                            owner.getType().toString(), 
                            "đang săn đuổi", 
                            prey.getType().toString()
                        );
                        logCooldown = 3.0; // 3 giây sau mới báo lại để tránh spam log
                    }
                    // ------------------------------------------------

                    double range = owner.getPosition().distance(prey.getPosition());

                    if (range < 5) {
                        owner.setAcceleration(new Vector2D(0, 0));
                        owner.setVelocity(new Vector2D(0, 0));
                        
                        // --- PHẦN THÊM: Báo tin khi bắt được mục tiêu ---
                        world.notifyAction(owner.getType().toString(), "đã bắt được", prey.getType().toString());
                        owner.eat(prey, dt);
                    } else {
                        // Ưu tiên sử dụng A* pathfinding
                        Vector2D ownerPos = owner.getPosition();
                        Vector2D preyPos = prey.getPosition();
                        List<Vector2D> path = world.findPathAStar(owner, ownerPos, preyPos);

                        Vector2D desiredVelocity = null;
                        if (path != null && !path.isEmpty()) {
                            DEBUG_PATH_STATES.put(owner.getId(), new DebugPathState(path));
                            Vector2D nextWaypoint = null;
                            for (Vector2D waypoint : path) {
                                if (waypoint != null && ownerPos.distance(waypoint) > 3) {
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

                        // Fallback: direct steering if no path found
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
                // Nếu không thấy mồi, reset cooldown để khi gặp mồi mới sẽ báo ngay
                logCooldown = 0;
                clearDebugPathState(owner.getId());

            }
        }
    }
}