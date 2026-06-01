package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.Herbivore;
import org.openjfx.app.entities.base.LivingEntity;

public class Fish extends Herbivore {
    private static final double REPRODUCE_INTERVAL = 30.0;
    private static final int MAX_FISH_COUNT = 20;
    private double reproduceTimer;

    public Fish(Vector2D position) {
        super(position, 20.0, "ellipse", 100.0, 0.5, 0.0,
                30.0, 20.0, 3.0, 35.0, 20.0);
        setVisionRadius(100.0);
        type = EntityType.FISH;
        matureAge = 3.0;
        reproduceCooldownMax = 8.0;
        reproduceHungerCost = 15.0;
    }

    @Override
    protected LivingEntity createOffspring(Vector2D spawnPos) {
        return new Fish(spawnPos);
    }

    @Override
    public void update(double dt, WorldMap world) {
        super.update(dt, world);
        reproduceInLake(dt, world);
    }

    private void reproduceInLake(double dt, WorldMap world) {
        if (!isAlive() || !world.canStandOn(this, getPosition())) {
            reproduceTimer = 0;
            return;
        }
        reproduceTimer += dt;
        if (reproduceTimer < REPRODUCE_INTERVAL || countFish(world) >= MAX_FISH_COUNT) return;

        reproduceTimer = 0;
        for (int i = 0; i < 12; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 20 + Math.random() * 60;
            Vector2D babyPosition = getPosition().add(new Vector2D(
                    Math.cos(angle) * distance,
                    Math.sin(angle) * distance));
            if (world.canStandOn(this, babyPosition)) {
                world.queueSpawn(new Fish(babyPosition));
                return;
            }
        }
    }

    private int countFish(WorldMap world) {
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Fish) count++;
        }
        return count;
    }

    @Override
    public String toString() {
        return "Fish#" + getId();
    }
}
