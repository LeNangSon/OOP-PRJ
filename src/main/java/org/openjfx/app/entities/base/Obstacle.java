package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.StaticEntity;

public abstract class Obstacle extends StaticEntity {

    public Obstacle(Vector2D position, double size, String shape) {
        super(position, size, shape);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Chướng ngại vật không làm gì
    }
}