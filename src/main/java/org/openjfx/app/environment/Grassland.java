package org.openjfx.app.environment;

import org.openjfx.app.core.Environment;
import org.openjfx.app.core.Vector2D;

public class Grassland extends Environment {
    public Grassland(Vector2D position, double width, double height) {
        super(position, width, height, "Grassland");
        // Có thể dùng EntityType.GRASS làm đại diện
    }
}