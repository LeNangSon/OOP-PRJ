package org.openjfx.app.core.terrain;

public class TerrainTile {
    private final int row;
    private final int col;
    private TerrainType type;

    public TerrainTile(int row, int col, TerrainType type) {
        this.row = row;
        this.col = col;
        this.type = type;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public TerrainType getType() {
        return type;
    }

    public void setType(TerrainType type) {
        this.type = type;
    }
}
