package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class FleeStrategy implements MoveStrategy {
    private static final double STEERING_GAIN   = 4.0;
    private static final double HIDE_DURATION = 1.0; // flag: đang ẩn khi sói thấy

    private double logCooldown = 0;
    private double hidingTimer = 0; // đếm ngược thời gian ẩn nấp

    public static final class DebugPathState {
        private final List<Vector2D> path;
        public DebugPathState(List<Vector2D> path) { this.path = path; }
        public List<Vector2D> getPath() { return path; }
    }

    private static final Map<Integer, DebugPathState> DEBUG_PATH_STATES = new ConcurrentHashMap<>();

    public static DebugPathState getDebugPathState(int entityId) {
        return DEBUG_PATH_STATES.get(entityId);
    }

    public static void clearDebugPathState(int entityId) {
        DEBUG_PATH_STATES.remove(entityId);
    }

    /** Trả về true khi thỏ đang ẩn trong bụi (dùng bởi Herbivore để giữ strategy) */
    public boolean isHiding() {
        return hidingTimer > 0;
    }

    public int findClosetThreat(LivingEntity owner, List<Entity> neighbors) {
        double minDistance = Double.MAX_VALUE;
        int closestID = -1;
        for (Entity neighbor : neighbors) {
            if (RelationManager.isScaredOf(owner.getType(), neighbor.getType())) {
                double distance = owner.getPosition().distance(neighbor.getPosition());
                if (distance <= minDistance) {
                    minDistance = distance;
                    closestID = neighbor.getId();
                }
            }
        }
        return closestID;
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (!owner.isAlive()) return;

        int mostDangerous = findClosetThreat(owner, neighbors);
        Vector2D ownerPos = owner.getPosition();

        // --- Đang trong bụi cây ---
        if (world.getTerrainAt(ownerPos) == TerrainType.BUSH) {
            if (mostDangerous != -1) {
                // Sói vẫn trong tầm nhìn → đứng yên, ẩn nấp
                hidingTimer = HIDE_DURATION;
                owner.setVelocity(new Vector2D(0, 0));
                owner.setAcceleration(new Vector2D(0, 0));
                clearDebugPathState(owner.getId());
                return;
            }
            // Sói đã đi xa → ra khỏi bụi ngay, không đứng yên nữa
            hidingTimer = 0;
        }

        // --- Không có mối đe dọa và không đang ẩn → wander bình thường ---
        if (mostDangerous == -1) {
            hidingTimer = 0;
            logCooldown = 0;
            clearDebugPathState(owner.getId());
            new WanderStrategy(owner.getVisionRadius() * 0.6, owner.getVisionRadius() * 0.35)
                    .updateVelocity(owner, neighbors, dt, world);
            return;
        }

        Entity threat = world.getEntityById(mostDangerous);
        if (threat == null) {
            clearDebugPathState(owner.getId());
            new WanderStrategy(owner.getVisionRadius() * 0.6, owner.getVisionRadius() * 0.35)
                    .updateVelocity(owner, neighbors, dt, world);
            return;
        }

        // --- Log ---
        logCooldown -= dt;
        if (logCooldown <= 0) {
            world.notifyAction(
                owner.getClass().getSimpleName() + "#" + owner.getId(),
                "đang chạy trốn khỏi",
                threat.getClass().getSimpleName() + "#" + threat.getId()
            );
            logCooldown = 2.5;
        }

        // --- Tìm bụi cây gần nhất để ẩn (toàn bản đồ) ---
        Vector2D destination = world.findNearestTerrainPosition(ownerPos, TerrainType.BUSH);

        Vector2D desiredVelocity = null;

        if (destination != null) {
            // Có bụi → chạy vào bụi theo A*
            List<Vector2D> path = world.findPathAStar(owner, ownerPos, destination);
            if (path != null && !path.isEmpty()) {
                DEBUG_PATH_STATES.put(owner.getId(), new DebugPathState(path));
                for (Vector2D waypoint : path) {
                    if (waypoint != null && ownerPos.distance(waypoint) > 1e-3) {
                        desiredVelocity = ownerPos.directionTo(waypoint).multiply(owner.getMaxSpeed());
                        break;
                    }
                }
            } else {
                clearDebugPathState(owner.getId());
            }
        } else {
            clearDebugPathState(owner.getId());
        }

        // Không tìm được bụi → chạy ngược chiều sói
        if (desiredVelocity == null) {
            desiredVelocity = threat.getPosition().directionTo(ownerPos).multiply(owner.getMaxSpeed());
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
