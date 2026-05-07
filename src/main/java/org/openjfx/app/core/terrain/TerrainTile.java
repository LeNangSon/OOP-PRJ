package org.openjfx.app.core.terrain;

public class TerrainTile {
    private final int x; // cột
    private final int y; // hàng
    private int tileId;  // ID từ file CSV (0.png, 1.png...)
    private TerrainType type;

    public TerrainTile(int x, int y, TerrainType type, int tileId) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.tileId = tileId;
    }

    public TerrainType getType() { return type; }
    public void setType(TerrainType type) { this.type = type; }
    
    public int getTileId() { return tileId; }
    public void setTileId(int tileId) { this.tileId = tileId; }

    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isWalkable() {
        return type.isWalkable();
    }
}