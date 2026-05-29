package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Bear extends Carnivore {

    public Bear(Vector2D position) {
        // Truyền đầy đủ 11 tham số lên lớp cha Carnivore:
        // position, size, shape, health, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
                30.0,      // size (Gấu khá to, nên để tầm 80)
                "circle",  // shape
                200.0,     // initialHealth (Gấu rất trâu)
                5.0,       // hungerRate (Tốc độ đói vừa phải)
                3.0,       // thirstRate
                45.0,      // maxSpeed (Chậm hơn sói nhưng nhanh hơn voi)
                30.0,       // maxForce (Lực lái tốt để vồ mồi)
                5.0,      // mass (Khối lượng lớn tạo độ đà)
                35.0,      // wanderDistance
                35.0       // wanderRadius
        );

        // Thiết lập tầm nhìn (radius) để phát hiện con mồi hoặc mối đe dọa
        this.setVisionRadius(40.0);
        this.setHunger(50);
        this.type = EntityType.BEAR;
        this.matureAge = 15.0;
        this.reproduceCooldownMax = 40.0;
        this.reproduceHungerCost = 35.0;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Bear(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Carnivore.update sẽ tự động điều phối các hành vi (Flee, Drink, Hunt, Wander)
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/bear.png";
    }
}