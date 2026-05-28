package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;

public class Rock extends Obstacle {

    public Rock(Vector2D position) {
        super(position, 15, "Rock");
    }

    @Override
    public String toString() {
        return "org/openjfx/app/rock.png";
    }
}
