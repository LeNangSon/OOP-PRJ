package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Carnivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Bear extends Carnivore {

    public Bear(Vector2D position) {
        super(position, 30, "circle", 200.0, 1.5, 2.0,
                36.0, 44.0, 12.0, 70.0, 35.0);
        setVisionRadius(80.0);
        type = EntityType.BEAR;
        matureAge = 15.0;
        reproduceCooldownMax = 40.0;
        reproduceHungerCost = 35.0;
    }

    public void setVisionRadius(double visionRadius) {
        this.visionRadius = visionRadius;
    }
    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Bear(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "Bear#" + getId();
    }
}
