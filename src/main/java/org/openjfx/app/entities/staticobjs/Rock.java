package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.entities.base.Obstacle;

public class Rock extends Obstacle {

    public Rock(Vector2D position, double size, String shape) {
        super(position, 15, "Rock");
    }
}