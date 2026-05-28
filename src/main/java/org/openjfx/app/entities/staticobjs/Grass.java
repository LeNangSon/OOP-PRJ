package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;

public class Grass extends Plant {

    public Grass(Vector2D position) {
        super(position, 10, "Grass", 15);
        this.type = EntityType.GRASS;
    }

    @Override
    protected Plant createNewPlant(Vector2D position) {
        return new Grass(position);
    }

    @Override
    protected boolean canReproduceAt(WorldMap world, Vector2D position) {
        return world.getTerrainAt(position) == TerrainType.LAND;
    }

    @Override
    public String toString() {
        return "org/openjfx/app/grass.png";
    }

}