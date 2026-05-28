package org.openjfx.app.entities.movable;

import org.openjfx.app.EntityConfig;
import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Elephant extends Herbivore {

    public Elephant(Vector2D position) {
        super(
                position,
                EntityConfig.Elephant.SIZE,
                "rect",
                EntityConfig.Elephant.HEALTH,
                EntityConfig.Elephant.HUNGER_RATE,
                EntityConfig.Elephant.THIRST_RATE,
                EntityConfig.Elephant.MAX_SPEED,
                EntityConfig.Elephant.MAX_FORCE,
                EntityConfig.Elephant.MASS,
                EntityConfig.Elephant.WANDER_DISTANCE,
                EntityConfig.Elephant.WANDER_RADIUS
        );

        this.setVisionRadius(EntityConfig.Elephant.VISION_RADIUS);
        this.type = EntityType.ELEPHANT;
        this.matureAge             = EntityConfig.Elephant.MATURE_AGE;
        this.reproduceCooldownMax  = EntityConfig.Elephant.REPRODUCE_COOLDOWN;
        this.reproduceHungerCost   = EntityConfig.Elephant.REPRODUCE_HUNGER_COST;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Elephant(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Herbivore.update sẽ xử lý logic chuyển đổi giữa Wander, SeekWater, Flee...
        super.update(dt, world);
    }

    @Override
    public String toString() {
        // Đảm bảo file này tồn tại trong resources
        return "org/openjfx/app/elephant.png";
    }
}