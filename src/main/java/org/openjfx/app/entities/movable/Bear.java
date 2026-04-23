package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;

public class Bear extends Carnivore {

    public Bear(Vector2D position) {
        // Truyền đầy đủ 11 tham số lên lớp cha Carnivore:
        // position, size, shape, health, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
                80.0,      // size (Gấu khá to, nên để tầm 80)
                "circle",  // shape
                200.0,     // initialHealth (Gấu rất trâu)
                1.5,       // hungerRate (Tốc độ đói vừa phải)
                2.0,       // thirstRate
                45.0,      // maxSpeed (Chậm hơn sói nhưng nhanh hơn voi)
                3.0,       // maxForce (Lực lái tốt để vồ mồi)
                30.0,      // mass (Khối lượng lớn tạo độ đà)
                70.0,      // wanderDistance
                35.0       // wanderRadius
        );

        // Thiết lập tầm nhìn (radius) để phát hiện con mồi hoặc mối đe dọa
        this.setRadius(200.0);
        this.type = EntityType.BEAR;
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Carnivore.update sẽ tự động điều phối các hành vi (Flee, Drink, Hunt, Wander)
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/Bear.png";
    }
}