package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;

public class Wolf extends Carnivore {

    public Wolf(Vector2D position) {
        // Gọi super của Carnivore với đầy đủ thông số:
        // position, size, shape, initialHealth, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
                30.0,      // size (Kích thước vừa phải)
                "circle",  // shape
                150.0,     // initialHealth (Sói khá trâu)
                2.0,       // hungerRate (Tốc độ đói)
                1.5,       // thirstRate (Tốc độ khát)
                70.0,      // maxSpeed (Sói chạy nhanh hơn thỏ và voi)
                4.0,       // maxForce (Khả năng bẻ lái khi đuổi mồi rất tốt)
                5.0,       // mass (Khối lượng trung bình giúp tăng tốc nhanh)
                80.0,      // wanderDistance (Khoảng cách điểm lang thang ảo)
                30.0       // wanderRadius (Bán kính nhiễu)
        );

        // Thiết lập tầm nhìn rộng để phát hiện thỏ từ xa
        this.setRadius(300.0);
        this.type = EntityType.WOLF;
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Carnivore.update sẽ tự động điều phối:
        // Thấy kẻ thù -> Chạy (Flee)
        // Khát -> Tìm nước (SeekWater)
        // Đói -> Đi săn (Hunter)
        // Rảnh -> Đi dạo (Wander)
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/Wolf.png";
    }
}