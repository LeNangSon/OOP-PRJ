package org.openjfx.app.entities.base;
import org.openjfx.app.core.Vector2D;

public class Grass extends Plant {
    public Grass(Vector2D position, double size, String shape, int regrowTime) {
        super(position, 5, "circle", 50);
        
        
         // size=5, shape=circle, regrowTime=50 ticks
    }
}
