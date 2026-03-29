package org.openjfx.app.entities.base;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Environment;
import org.openjfx.app.core.Vector2D;

public class Lake extends Environment {
    public Lake(Vector2D position, double width, double height) {
        super(position, width, height, "Lake");
        this.type = EntityType.WATER; 
    }
}