package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;

public class Rock extends Obstacle {

    public Rock(Vector2D position, double size, String shape) {
        super(position, 15, "Rock");
    }
}