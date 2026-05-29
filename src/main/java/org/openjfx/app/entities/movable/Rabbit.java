package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Rabbit extends Herbivore {

    public Rabbit(Vector2D position) {
        // Gọi super của Herbivore với đầy đủ thông số:
        // position, size, shape, initialHealth, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
            28,
                "circle",
                100.0,
                3.0,
                1.0,
            20,
            20,
            0.5,
            10.0,
            10.0
        );

        // Thiết lập tầm nhìn (radius để quét neighbors)
        this.setVisionRadius(40.0); // Tầm nhìn xa hơn để phản ứng sớm trên map 1032x576
        this.type = EntityType.RABBIT;
        // Thỏ sinh sản rất nhanh: trưởng thành sớm, cooldown ngắn, ít tốn hunger.
        this.matureAge = 4.0;
        this.reproduceCooldownMax = 10.0;
        this.reproduceHungerCost = 10.0;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Rabbit(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Chỉ cần gọi super.update của Herbivore
        // Vì Herbivore đã chứa logic xử lý Flee, SeekWater, Hunter, Wander
        super.update(dt, world);
    }

    @Override
    public String toString() {
        // Đường dẫn đến tài nguyên hình ảnh
        return "org/openjfx/app/rabbit.png";
    }
}