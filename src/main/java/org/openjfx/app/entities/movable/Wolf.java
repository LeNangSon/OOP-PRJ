package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.entities.base.Carnivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Wolf extends Carnivore {

    public Wolf(Vector2D position) {
        super(
                position,
                20.0,      // size (chiều cao hiển thị)
                "circle",  // shape
                200.0,     // initialHealth
                3.0,       // hungerRate
                0.0,       // thirstRate
                40.0,      // maxSpeed
                80.0,       // maxForce
                3.0,       // mass
                30.0,      // wanderDistance
                30.0       // wanderRadius
        );

        this.setVisionRadius(100.0);
        this.type = EntityType.WOLF;
        
        // THAY ĐỔI Ở ĐÂY: Gán trực tiếp vào biến để tránh lỗi không tìm thấy method
        // Đảm bảo HunterStrategy được thực thi để bắn Log ra Terminal cho Nam
        this.moveStrategy = new HunterStrategy();
        this.matureAge = 10.0;
        this.reproduceCooldownMax = 30.0;
        this.reproduceHungerCost = 35.0;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Wolf(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Logic của Carnivore sẽ tự động điều phối hành vi
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/wolf.png";
    }
}