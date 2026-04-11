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
            20,      // size
                "circle",  // shape
                100.0,     // initialHealth
                0.0,       // hungerRate (test wander-only)
                0.0,       // thirstRate (test wander-only)
            20,      // maxSpeed (Thỏ cần nhanh hơn sói để có cơ hội thoát)
            15,       // maxForce (Bẻ lái tốt hơn trên map có nhiều chướng ngại)
            0.5,       // mass (Giữ độ linh hoạt cao)
            10.0,      // wanderDistance (Đi ổn định hơn trên map rộng)
            10.0       // wanderRadius (Giảm lắc khi lang thang gần vật cản)
        );

        // Thiết lập tầm nhìn (radius để quét neighbors)
        this.setRadius(180.0); // Tầm nhìn xa hơn để phản ứng sớm trên map 1032x576
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