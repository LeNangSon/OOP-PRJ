package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.*;

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
        // Logic ăn thịt: Thường là kiểm tra target có phải LivingEntity không
        // và cộng máu/giảm đói dựa trên kích thước con mồi
        if (target instanceof LivingEntity) {
            LivingEntity prey = (LivingEntity) target;
            if (!prey.isAlive()) {
                this.setHunger(this.getHunger() - 50.0); // Ví dụ: ăn xong giảm 50 đơn vị đói
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
            // Trạng thái bình thường: Đi tuần tra (Wander)
            if (!(this.moveStrategy instanceof WanderStrategy)) {
                this.moveStrategy = new WanderStrategy(this.wanderDistance, this.wanderRadius);
            }
        }

        // Thực thi tính toán vận tốc
        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, neighbors, dt, world);
        }

        // Cập nhật vị trí và các chỉ số sinh tồn cơ bản
        super.update(dt, world);
    }
}