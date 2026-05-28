package org.openjfx.app.entities.movable;

import org.openjfx.app.EntityConfig;
import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Rabbit extends Herbivore {

    public Rabbit(Vector2D position) {
        super(
                position,
                EntityConfig.Rabbit.SIZE,
                "circle",
                EntityConfig.Rabbit.HEALTH,
                EntityConfig.Rabbit.HUNGER_RATE,
                EntityConfig.Rabbit.THIRST_RATE,
                EntityConfig.Rabbit.MAX_SPEED,
                EntityConfig.Rabbit.MAX_FORCE,
                EntityConfig.Rabbit.MASS,
                EntityConfig.Rabbit.WANDER_DISTANCE,
                EntityConfig.Rabbit.WANDER_RADIUS
        );

        this.setVisionRadius(EntityConfig.Rabbit.VISION_RADIUS);
        this.type = EntityType.RABBIT;
        this.matureAge             = EntityConfig.Rabbit.MATURE_AGE;
        this.reproduceCooldownMax  = EntityConfig.Rabbit.REPRODUCE_COOLDOWN;
        this.reproduceHungerCost   = EntityConfig.Rabbit.REPRODUCE_HUNGER_COST;
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