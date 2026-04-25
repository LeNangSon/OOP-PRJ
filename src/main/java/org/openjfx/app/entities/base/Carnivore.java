package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.core.strategies.SeekWaterStrategy;
import org.openjfx.app.core.strategies.WanderStrategy;

public abstract class Carnivore extends LivingEntity {

    public Carnivore(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate,
                     double maxSpeed, double maxForce, double mass,
                     double wanderDistance, double wanderRadius) {
        // Truyền đầy đủ thông số lên LivingEntity
        super(position, size, shape, initialHealth, hungerRate, thirstRate,
                maxSpeed, maxForce, mass, wanderDistance, wanderRadius);
    }

    @Override
    public void eat(Entity target, double dt) {
        if (target instanceof LivingEntity) {
            LivingEntity prey = (LivingEntity) target;
            if (prey.isAlive()) {
                prey.setHealth(0);
                this.setHunger(0);
                this.setHealth(200);
                this.setVelocity(new Vector2D(0, 0));
            }
        }
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Cập nhật danh sách hàng xóm dựa trên tầm nhìn (radius)
        this.neighbors = world.getNeighbors(this, this.radius);

        // Quyết định chiến thuật di chuyển
        if (hasThreat(this, neighbors)) {
            // Thú ăn thịt vẫn có thể sợ kẻ thù lớn hơn hoặc con người
            if (!(this.moveStrategy instanceof FleeStrategy)) {
                this.moveStrategy = new FleeStrategy();
            }
        } else if (this.getThirst() > 70.0) {
            if (!(this.moveStrategy instanceof SeekWaterStrategy)) {
                this.moveStrategy = new SeekWaterStrategy(this.wanderDistance, this.wanderRadius);
            }
        } else if (this.getHunger() > 60.0) { // Thú ăn thịt thường đi săn sớm hơn
            if (!(this.moveStrategy instanceof HunterStrategy)) {
                this.moveStrategy = new HunterStrategy();
            }
        } else {
            // Trạng thái bình thường: Wander
            if (!(this.moveStrategy instanceof WanderStrategy)) {
                this.moveStrategy = new WanderStrategy(this.wanderDistance, this.wanderRadius);
            }
        }

        // Tính toán vận tốc
        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, neighbors, dt, world);
        }

        // Cập nhật vị trí và các chỉ số sinh tồn cơ bản
        super.update(dt, world);
    }
}