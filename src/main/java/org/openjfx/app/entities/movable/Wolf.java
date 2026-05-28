package org.openjfx.app.entities.movable;

import org.openjfx.app.EntityConfig;
import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.entities.base.Carnivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Wolf extends Carnivore {

    public Wolf(Vector2D position) {
        super(
                position,
                EntityConfig.Wolf.SIZE,
                "circle",
                EntityConfig.Wolf.HEALTH,
                EntityConfig.Wolf.HUNGER_RATE,
                EntityConfig.Wolf.THIRST_RATE,
                EntityConfig.Wolf.MAX_SPEED,
                EntityConfig.Wolf.MAX_FORCE,
                EntityConfig.Wolf.MASS,
                EntityConfig.Wolf.WANDER_DISTANCE,
                EntityConfig.Wolf.WANDER_RADIUS
        );

        this.setVisionRadius(EntityConfig.Wolf.VISION_RADIUS);
        this.type = EntityType.WOLF;
        this.moveStrategy          = new HunterStrategy();
        this.matureAge             = EntityConfig.Wolf.MATURE_AGE;
        this.reproduceCooldownMax  = EntityConfig.Wolf.REPRODUCE_COOLDOWN;
        this.reproduceHungerCost   = EntityConfig.Wolf.REPRODUCE_HUNGER_COST;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Wolf(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Logic của Carnivore sẽ tự động điều phối hành vi
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/wolf.png";
    }
}