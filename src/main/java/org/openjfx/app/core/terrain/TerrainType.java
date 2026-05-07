package org.openjfx.app.core.terrain;

public enum TerrainType {
    LAND(true, false),      // Co
    WATER(false, true),    // Song_duongdi (vùng nước)
    PATH(true, false),      // Song_duongdi (đường đi)
    BUSH(true, false),      // Co (bụi rậm)
    OBSTACLE(false, false), // Da_buihoa
    ROCK(false, false);     // Da_buihoa

    private final boolean isWalkable;
    private final boolean isWater;

    TerrainType(boolean isWalkable, boolean isWater) {
        this.isWalkable = isWalkable;
        this.isWater = isWater;
    }

    public boolean isWalkable() { return isWalkable; }
    public boolean isWater() { return isWater; }
}