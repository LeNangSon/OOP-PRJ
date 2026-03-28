package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.entities.base.Obstacle;

public class Bush extends Obstacle {

    public Bush(Vector2D position) {
        super(position, 10, "Bush");
    }
}