package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;

public class Grass extends Plant {

    private static final double MIN_DISTANCE_FROM_GRASS = 18.0;

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
        if (world.getTerrainAt(position) != TerrainType.LAND) {
            return false;
        }
        double minSq = MIN_DISTANCE_FROM_GRASS * MIN_DISTANCE_FROM_GRASS;
        for (Entity e : world.getEntities()) {
            if (!(e instanceof Grass) || !((Grass) e).isAlive()) {
                continue;
            }
            double dx = e.getPosition().x - position.x;
            double dy = e.getPosition().y - position.y;
            if (dx * dx + dy * dy < minSq) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "org/openjfx/app/grass.png";
    }

}