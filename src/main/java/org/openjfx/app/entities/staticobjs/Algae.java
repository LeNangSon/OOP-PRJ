package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;

public class Algae extends Plant {

    public Algae(Vector2D position) {
        super(position, 10, "Algae", 5, 10);
        this.type = EntityType.ALGAE;
    }

    @Override
    protected Plant createNewPlant(Vector2D position) {
        return new Algae(position);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/algea.png";
    }
}