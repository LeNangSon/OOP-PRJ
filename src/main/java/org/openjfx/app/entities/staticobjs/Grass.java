package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;

public class Grass extends Plant {

    private static final double MIN_DISTANCE_FROM_GRASS = 18.0;
    // Bán kính an toàn quanh cỏ — không được có đá/sông trong vùng này.
    private static final double HAZARD_BUFFER = 24.0;

    public Grass(Vector2D position) {
        super(position, 10, "Grass", 25);
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
        if (hasNearbyHazard(world, position)) {
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

    /** Kiểm tra 8 điểm quanh vị trí — nếu có ROCK/WATER/PIT thì không mọc được. */
    private boolean hasNearbyHazard(WorldMap world, Vector2D position) {
        double[][] offsets = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1},
        };
        for (double[] o : offsets) {
            Vector2D probe = new Vector2D(
                    position.x + o[1] * HAZARD_BUFFER,
                    position.y + o[0] * HAZARD_BUFFER);
            TerrainType t = world.getTerrainAt(probe);
            if (t == TerrainType.ROCK || t == TerrainType.WATER || t == TerrainType.PIT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "org/openjfx/app/grass.png";
    }

}