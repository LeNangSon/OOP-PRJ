package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.*;
import org.openjfx.app.entities.staticobjs.Plant;

public abstract class Herbivore extends LivingEntity {

    public Herbivore(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate,
                     double maxSpeed, double maxForce, double mass,
                     double wanderDistance, double wanderRadius) {
        super(position, size, shape, initialHealth, hungerRate, thirstRate, maxSpeed, maxForce, mass, wanderDistance, wanderRadius);
    }

    @Override
    public void eat(Entity target, double dt) {
        if (target instanceof Plant) {
            setHunger(this.getHunger() - ((Plant) target).consume());
        }
    }

    @Override
    public void update(double dt, WorldMap world) {
        this.neighbors = world.getNeighbors(this, this.radius);

        // Quyết định chiến thuật dựa trên nhu cầu
        if (hasThreat(this, neighbors)) {
            if (!(this.moveStrategy instanceof FleeStrategy)) {
                this.moveStrategy = new FleeStrategy();
            }
        } else if (this.getThirst() > 70.0) {
            if (!(this.moveStrategy instanceof SeekWaterStrategy)) {
                this.moveStrategy = new SeekWaterStrategy(this.wanderDistance, this.wanderRadius);
            }
        } else if (this.getHunger() > 70.0) {
            if (!(this.moveStrategy instanceof HunterStrategy)) {
                this.moveStrategy = new HunterStrategy();
            }
        } else {
            // Quay lại trạng thái lang thang nếu không có nhu cầu cấp bách
            if (!(this.moveStrategy instanceof WanderStrategy)) {
                this.moveStrategy = new WanderStrategy(this.wanderDistance, this.wanderRadius);
            }
        }

        // Cập nhật vận tốc từ Strategy
        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, neighbors, dt, world);
        }

        // Cập nhật vị trí và các chỉ số sinh tồn (Gọi lớp cha)
        super.update(dt, world);
    }
}