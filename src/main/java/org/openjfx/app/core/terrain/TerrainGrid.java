package org.openjfx.app.core.terrain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openjfx.app.core.Vector2D;

public class TerrainGrid {
    // Mapping for Tiled CSV numeric IDs.
    // Update this map if your tileset IDs change.
    private static final Map<Integer, TerrainType> NUMERIC_TILE_MAPPING = new HashMap<>();
    static {
        NUMERIC_TILE_MAPPING.put(0, TerrainType.WATER);
        NUMERIC_TILE_MAPPING.put(1, TerrainType.BUSH);
        NUMERIC_TILE_MAPPING.put(2, TerrainType.LAND);
        NUMERIC_TILE_MAPPING.put(3, TerrainType.PIT);
        NUMERIC_TILE_MAPPING.put(4, TerrainType.ROCK);
    }

    private final int tileSize;
    private final int rows;
    private final int cols;
    private final TerrainTile[][] tiles;

    private TerrainGrid(int tileSize, int rows, int cols) {
        this.tileSize = tileSize;
        this.rows = rows;
        this.cols = cols;
        this.tiles = new TerrainTile[rows][cols];
    }

    public static TerrainGrid fromCsvResource(String resourcePath, int tileSize) {
        try (InputStream in = TerrainGrid.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Cannot find terrain csv: " + resourcePath);
            }

            List<String[]> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        lines.add(trimmed.split("\\s*,\\s*"));
                    }
                }
            }

            if (lines.isEmpty()) {
                throw new IllegalArgumentException("Terrain csv is empty: " + resourcePath);
            }

            int rows = lines.size();
            int cols = lines.get(0).length;
            TerrainGrid grid = new TerrainGrid(tileSize, rows, cols);

            for (int r = 0; r < rows; r++) {
                String[] row = lines.get(r);
                if (row.length != cols) {
                    throw new IllegalArgumentException("Inconsistent column count in terrain csv at row " + r);
                }
                for (int c = 0; c < cols; c++) {
                    TerrainType type = decode(row[c]);
                    grid.tiles[r][c] = new TerrainTile(r, c, type);
                }
            }
            return grid;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load terrain grid from " + resourcePath, e);
        }
    }

    private static TerrainType decode(String code) {
        String normalized = code.trim().toUpperCase();

        // Support Tiled CSV numeric IDs (e.g., 0,1,2,3,4)
        try {
            int numericId = Integer.parseInt(normalized);
            return NUMERIC_TILE_MAPPING.getOrDefault(numericId, TerrainType.LAND);
        } catch (NumberFormatException ignored) {
            // Not numeric, continue to alphabetic mapping below.
        }

        switch (normalized) {
            case "W":
                return TerrainType.WATER;
            case "R":
                return TerrainType.ROCK;
            case "B":
                return TerrainType.BUSH;
            case "H":
                return TerrainType.PIT;
            case "L":
            default:
                return TerrainType.LAND;
        }
    }

    public TerrainType getTerrainAt(Vector2D worldPosition) {
        int col = (int) (worldPosition.x / tileSize);
        int row = (int) (worldPosition.y / tileSize);
        if (!isInside(row, col)) {
            return TerrainType.ROCK;
        }
        return tiles[row][col].getType();
    }

    public TerrainTile getTile(int row, int col) {
        if (!isInside(row, col)) {
            return null;
        }
        return tiles[row][col];
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }
}
