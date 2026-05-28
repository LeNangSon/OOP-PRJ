package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;

public class Algae extends Plant {

    // Fish hungerRate = 0.5/giây -> hunger 0->70 = 140 giây.
    // Tốc độ sinh sản tảo khớp đúng nhịp đó để cá luôn có đủ thức ăn vừa phải.
    private static final double REPRODUCE_TIME_SECONDS = 140.0;

    public Algae(Vector2D position) {
        super(position, 10, "Algae", REPRODUCE_TIME_SECONDS);
        this.type = EntityType.ALGAE;
    }

    @Override
    protected Plant createNewPlant(Vector2D position) {
        return new Algae(position);
    }

    @Override
    protected boolean canReproduceAt(WorldMap world, Vector2D position) {
        return world.getTerrainAt(position) == TerrainType.WATER;
    }

    @Override
    public String toString() {
        return "org/openjfx/app/algea.png";
    }
}
