package org.openjfx.app.entities.movable;

import org.openjfx.app.EntityConfig;
import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Bear extends Carnivore {

    public Bear(Vector2D position) {
        super(
                position,
                EntityConfig.Bear.SIZE,
                "circle",
                EntityConfig.Bear.HEALTH,
                EntityConfig.Bear.HUNGER_RATE,
                EntityConfig.Bear.THIRST_RATE,
                EntityConfig.Bear.MAX_SPEED,
                EntityConfig.Bear.MAX_FORCE,
                EntityConfig.Bear.MASS,
                EntityConfig.Bear.WANDER_DISTANCE,
                EntityConfig.Bear.WANDER_RADIUS
        );

        this.setVisionRadius(EntityConfig.Bear.VISION_RADIUS);
        this.type = EntityType.BEAR;
        this.matureAge             = EntityConfig.Bear.MATURE_AGE;
        this.reproduceCooldownMax  = EntityConfig.Bear.REPRODUCE_COOLDOWN;
        this.reproduceHungerCost   = EntityConfig.Bear.REPRODUCE_HUNGER_COST;
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