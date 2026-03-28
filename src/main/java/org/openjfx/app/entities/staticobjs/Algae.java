package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.entities.base.Plant;

public class Algae extends Plant {

    public Algae(Vector2D position) {
        super(position, 4, "Algae", 5, 10);
        // mọc nhanh hơn cỏ
    }

    @Override
    protected Plant createNewPlant(Vector2D position) {
        return new Algae(position);
    }
}