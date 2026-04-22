package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class HunterStrategy implements MoveStrategy {

    private static final double STEERING_GAIN = 4.0;

    public HunterStrategy() {
    }

    /**
     * Tìm con mồi gần nhất trong danh sách hàng xóm
     */
    public int findClosestPrey(LivingEntity owner, List<Entity> neighbors) {
        double minDistance = Double.MAX_VALUE;
        int closestID = -1;

        for (Entity neighbor : neighbors) {
            // Kiểm tra xem hàng xóm này có phải là con mồi của chủ thể không
            if (RelationManager.isPrey(neighbor.getType(), owner.getType())) {
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
            int targetId = findClosestPrey(owner, neighbors);

            if (targetId != -1) {
                Entity prey = world.getEntityById(targetId);
                if (prey != null) {
                    double range = owner.getPosition().distance(prey.getPosition());

                    if (range < 5) {
                        owner.setAcceleration(new Vector2D(0, 0));
                        owner.setVelocity(new Vector2D(0, 0));
                        owner.eat(prey, dt);
                    } else {
                        Vector2D desiredVelocity = owner.getPosition().directionTo(prey.getPosition()).multiply(owner.getMaxSpeed());
                        Vector2D steering = desiredVelocity.sub(owner.getVelocity());
                        Vector2D acceleration = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
                        Vector2D newVelocity = owner.getVelocity().add(acceleration.multiply(dt)).limit(owner.getMaxSpeed());
                        owner.setAcceleration(acceleration);
                        owner.setVelocity(newVelocity);
                    }

                }
            } else {
                // Nếu không có con mồi, đứng yên hoặc có thể đổi sang chiến thuật đi tuần (Wander)
                
            }
        }
    }
}