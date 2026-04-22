package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class HunterStrategy implements MoveStrategy {

    // Biến tạm để kiểm soát tần suất gửi log (tránh tràn terminal)
    private double logCooldown = 0;

    public HunterStrategy() {
    }

    /**
     * Tìm con mồi gần nhất trong danh sách hàng xóm (Giữ nguyên)
     */
    public int findClosestPrey(LivingEntity owner, List<Entity> neighbors) {
        double minDistance = Double.MAX_VALUE;
        int closestID = -1;

        for (Entity neighbor : neighbors) {
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
                    Vector2D directionToAttack = owner.getPosition().directionTo(prey.getPosition());
                    double range = owner.getPosition().distance(prey.getPosition());
                    
                    if (range < 5) {
                        owner.eat(prey, dt);
                        
                        // THÊM SỰ KIỆN: Thông báo đang săn mồi/ăn thịt
                        logCooldown -= dt;
                        if (logCooldown <= 0) {
                            world.notifyAction(
                                owner.getType().toString(), 
                                "đang săn đuổi", 
                                prey.getType().toString() + " (ID:" + prey.getId() + ")"
                            );
                            logCooldown = 1.5; // Gửi lại log sau 1.5 giây nếu vẫn đang ăn
                        }
                        
                    } else {
                        owner.setVelocity(directionToAttack.multiply(owner.getMaxSpeed()));
                    }
                }
            } else {
                // Reset cooldown khi không còn mục tiêu để khi gặp mục tiêu mới sẽ log ngay
                logCooldown = 0;
            }
        }
    }
}