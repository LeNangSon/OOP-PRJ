package org.openjfx.app.entities.movable;

import org.openjfx.app.EntityConfig;
import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Fish extends Herbivore {

    public Fish(Vector2D position) {
        super(
                position,
                EntityConfig.Fish.SIZE,
                "ellipse",
                EntityConfig.Fish.HEALTH,
                EntityConfig.Fish.HUNGER_RATE,
                EntityConfig.Fish.THIRST_RATE,
                EntityConfig.Fish.MAX_SPEED,
                EntityConfig.Fish.MAX_FORCE,
                EntityConfig.Fish.MASS,
                EntityConfig.Fish.WANDER_DISTANCE,
                EntityConfig.Fish.WANDER_RADIUS
        );

        this.setVisionRadius(EntityConfig.Fish.VISION_RADIUS);
        this.type = EntityType.FISH;
        this.matureAge             = EntityConfig.Fish.MATURE_AGE;
        this.reproduceCooldownMax  = EntityConfig.Fish.REPRODUCE_COOLDOWN;
        this.reproduceHungerCost   = EntityConfig.Fish.REPRODUCE_HUNGER_COST;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Fish(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Herbivore.update đã có logic: if (hasThreat) -> Flee, else if (Hungry) -> Hunter...
        super.update(dt, world);
    }

    @Override
    public String toString() {
        // Đảm bảo đường dẫn này khớp với file trong resources
        return "org/openjfx/app/fish.png";
    }
}