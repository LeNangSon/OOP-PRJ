package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.HunterStrategy;

public abstract class Carnivore extends LivingEntity {

    public Carnivore(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate,
                     double maxSpeed, double maxForce, double mass,
                     double wanderDistance, double wanderRadius) {
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

    // --- CẤU HÌNH TÍNH ĐIỂM RIÊNG CHO ĐỘNG VẬT ĂN THỊT ---

    @Override
    protected double evaluateEatScore() {
        // Thú ăn thịt chỉ bắt đầu đi săn khi độ đói vượt 60
        if (this.getHunger() > 60.0) {
            return this.getHunger() + 20.0; // Cộng thêm điểm bouns để ưu tiên săn mồi
        }
        return 0.0;
    }

    @Override
    protected double evaluateDrinkScore() {
        // Thú ăn thịt ưu tiên uống nước khi khát vượt 70
        if (this.getThirst() > 70.0 || (this.currentState == ActionState.DRINK && this.getThirst() > 0.1)) {
            return this.getThirst() + 20.0; 
        }
        return 0.0;
    }

    @Override
    protected void applyStrategyForState(ActionState state) {
        if (state == ActionState.EAT) {
            // Khi trạng thái là EAT, Thú ăn thịt sẽ dùng chiến thuật Hunter
            this.setMoveStrategy(new HunterStrategy());
        } else {
            // Các trạng thái khác trả về mặc định của LivingEntity
            super.applyStrategyForState(state); 
        }
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Gọi lên lớp cha (LivingEntity) để cập nhật sinh tồn và cho AI quyết định hành động
        super.update(dt, world);

        // Chạy Strategy di chuyển đã được AI quyết định
        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, this.neighbors, dt, world);
        }
    }
}