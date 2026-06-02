package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.entities.base.Carnivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Wolf extends Carnivore {

    public Wolf(Vector2D position) {
        super(position, 30.0, "circle", 200.0, 3.0, 5.0,
                35.0, 70.0, 3.0, 30.0, 30.0);
        setVisionRadius(50.0);
        type = EntityType.WOLF;
        moveStrategy = new HunterStrategy();
        matureAge = 10.0;
        reproduceCooldownMax = 30.0;
        reproduceHungerCost = 35.0;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Wolf(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "Wolf#" + getId();
    }
}
