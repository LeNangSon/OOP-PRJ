package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Rabbit extends Herbivore {

    public Rabbit(Vector2D position) {
        // Gọi super của Herbivore với đầy đủ thông số:
        // position, size, shape, initialHealth, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
                10,       // size (nhỏ để lọt vào chỗ trốn)
                "circle",
                100.0,    // health
                0.0,      // hungerRate
                5.0,      // thirstRate
                32,       // maxSpeed (tăng từ 20 → 32 để có cơ hội trốn)
                38,       // maxForce (tăng để rẽ nhanh hơn)
                0.5,      // mass
                15.0,     // wanderDistance
                12.0      // wanderRadius
        );

        this.setVisionRadius(190.0); // tăng từ 100 → 190 để phát hiện sói sớm hơn
        this.type = EntityType.RABBIT;
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
        return "org/openjfx/app/Rabbit.png";
    }
}