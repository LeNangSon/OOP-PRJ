package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Elephant extends Herbivore {

    public Elephant(Vector2D position) {
        // Gọi super của Herbivore với các thông số đặc trưng cho Voi:
        // position, size, shape, health, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
                120.0,     // size (Kích thước lớn)
                "rect",    // shape (Hình chữ nhật)
                100.0,     // initialHealth
                5.0,       // hungerRate (Voi ăn nhiều nên tốc độ đói nhanh)
                6.0,       // thirstRate (Voi uống nhiều nước)
                30.0,      // maxSpeed (Voi di chuyển chậm hơn thỏ)
                2.0,       // maxForce (Lực lái vừa phải)
                50.0,      // mass (Trọng lượng rất nặng, khó tăng tốc đột ngột)
                100.0,     // wanderDistance (Khoảng cách điểm ảo xa để đi thẳng ổn định)
                40.0       // wanderRadius (Bán kính nhiễu lớn hơn để quẹo vòng cung rộng)
        );

        // Thiết lập tầm nhìn (radius để quét neighbors)
        this.setRadius(150.0);

        // Bạn muốn voi bắt đầu trong trạng thái hơi khát để test SeekWater
        this.setThirst(80.0);

        this.type = EntityType.ELEPHANT;
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Herbivore.update sẽ xử lý logic chuyển đổi giữa Wander, SeekWater, Flee...
        super.update(dt, world);
    }

    @Override
    public String toString() {
        // Đảm bảo file này tồn tại trong resources
        return "org/openjfx/app/Elephant.png";
    }
}