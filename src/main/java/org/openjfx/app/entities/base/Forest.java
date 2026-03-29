package org.openjfx.app.entities.base;

import org.openjfx.app.core.Environment;
import org.openjfx.app.core.Vector2D;

public class Forest extends Environment {
    public Forest(Vector2D position, double width, double height) {
        super(position, width, height, "Forest");
    }
}