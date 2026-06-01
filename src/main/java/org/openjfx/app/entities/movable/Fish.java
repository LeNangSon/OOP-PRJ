package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.Herbivore;

public class Fish extends Herbivore {
    private static final double REPRODUCE_INTERVAL = 30.0;
    private static final int MAX_FISH_COUNT = 20;
    private double reproduceTimer;

    public Fish(Vector2D position) {
        // Truyền đầy đủ 11 tham số lên lớp cha Herbivore:
        // position, size, shape, health, hungerRate, thirstRate,
        // maxSpeed, maxForce, mass, wanderDistance, wanderRadius
        super(
                position,
                20.0,      // size
                "ellipse", // shape
                100.0,     // initialHealth
                0.5,       // hungerRate (Cá ăn ít, đói chậm)
                0.0,       // thirstRate (Cá không khát nước vì sống trong nước)
                30.0,      // maxSpeed
                20.0,      // maxForce (tăng 2.5→20 để cá rẽ được)
                3.0,       // mass
                35.0,      // wanderDistance
                20.0       // wanderRadius
        );

        this.setVisionRadius(100.0); // tăng 80→100
        this.type = EntityType.FISH;
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Herbivore.update đã có logic: if (hasThreat) -> Flee, else if (Hungry) -> Hunter...
        super.update(dt, world);
        reproduceInLake(dt, world);
    }

    private void reproduceInLake(double dt, WorldMap world) {
        if (!isAlive() || !world.canStandOn(this, getPosition())) {
            reproduceTimer = 0;
            return;
        }

        reproduceTimer += dt;
        if (reproduceTimer < REPRODUCE_INTERVAL || countFish(world) >= MAX_FISH_COUNT) {
            return;
        }

        reproduceTimer = 0;
        for (int i = 0; i < 12; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 20 + Math.random() * 60;
            Vector2D babyPosition = getPosition().add(new Vector2D(
                    Math.cos(angle) * distance,
                    Math.sin(angle) * distance
            ));
            if (world.canStandOn(this, babyPosition)) {
                world.addEntity(new Fish(babyPosition));
                return;
            }
        }
    }

    private int countFish(WorldMap world) {
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Fish) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        // Đảm bảo đường dẫn này khớp với file trong resources
        return "org/openjfx/app/Fish.png";
    }
}
