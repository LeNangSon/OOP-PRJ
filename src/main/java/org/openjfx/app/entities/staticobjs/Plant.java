package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.StaticEntity;

public abstract class Plant extends StaticEntity {
    protected boolean isAvailable;
    protected double regrowTime;
    protected double regrowTimer;

    protected double reproduceTime;
    protected double reproduceTimer;

    public Plant(Vector2D position, double size, String shape,
                 double regrowTime, double reproduceTime) {
        super(position, size, shape);
        this.regrowTime = regrowTime;
        this.reproduceTime = reproduceTime;

        this.isAvailable = true;
        this.regrowTimer = 0;
        this.reproduceTimer = 0;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void consume() {
        if (isAvailable) {
            isAvailable = false;
            regrowTimer = 0;
        }
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Mọc lại sau khi bị ăn
        if (!isAvailable) {
            regrowTimer += dt;
            if (regrowTimer >= regrowTime) {
                isAvailable = true;
                regrowTimer = 0;
            }
        }

        // Sinh sản
        reproduceTimer += dt;
        if (reproduceTimer >= reproduceTime && isAvailable) {
            reproduce(world);
            reproduceTimer = 0;
        }
    }

    protected void reproduce(WorldMap world) {
        double newX = position.x + (Math.random() * 60 - 30);
        double newY = position.y + (Math.random() * 60 - 30);
        world.addEntity(createNewPlant(new Vector2D(newX, newY)));
    }

    protected abstract Plant createNewPlant(Vector2D position);
}