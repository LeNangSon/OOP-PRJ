package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Rabbit extends Herbivore {

    public Rabbit(Vector2D position) {
        super(position, 10.0, "circle", 100.0, 2.0, 5.0,
                32.0, 38.0, 0.5, 35.0, 12.0);
        setVisionRadius(50.0);
        type = EntityType.RABBIT;
        matureAge = 4.0;
        reproduceCooldownMax = 10.0;
        reproduceHungerCost = 10.0;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Rabbit(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "Rabbit#" + getId();
    }
}
