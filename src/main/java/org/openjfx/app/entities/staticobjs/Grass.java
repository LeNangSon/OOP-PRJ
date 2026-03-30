package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;

public class Grass extends Plant {

    public Grass(Vector2D position) {
        super(position, 5, "Grass", 10, 15);
        // mọc lại 10s, sinh sản 15s
    }

    @Override
    protected Plant createNewPlant(Vector2D position) {
        return new Grass(position);
    }
    

}