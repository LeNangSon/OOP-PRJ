package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;

public class Grass extends Plant {

    public Grass(Vector2D position) {
        super(position, 10, "Grass", 10, 15);
        this.type = EntityType.GRASS;
    }

    @Override
    protected Plant createNewPlant(Vector2D position) {
        return new Grass(position);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/grass.png";
    }

}