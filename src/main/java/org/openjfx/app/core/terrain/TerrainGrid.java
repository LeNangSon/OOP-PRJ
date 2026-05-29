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
    public static final class GridCoordinate {
        private final int row;
        private final int col;

        public GridCoordinate(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }

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
            List<String[]> lines = readCsv(in, resourcePath);

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

    public static TerrainGrid fromLayeredCsvResources(
            String grassResourcePath,
            String waterResourcePath,
            String roadResourcePath,
            int tileSize) {
        try {
            List<String[]> grass = readCsvResource(grassResourcePath);
            List<String[]> water = readCsvResource(waterResourcePath);
            List<String[]> road = readCsvResource(roadResourcePath);
            validateSameSize(grass, water, waterResourcePath);
            validateSameSize(grass, road, roadResourcePath);

            int rows = grass.size();
            int cols = grass.get(0).length;
            TerrainGrid grid = new TerrainGrid(tileSize, rows, cols);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    TerrainType type = TerrainType.LAND;
                    if (hasTile(water.get(r)[c])) {
                        type = isMiddleOfCsvRun(water, r, c, 3)
                                ? TerrainType.WATER
                                : TerrainType.ROCK;
                    }
                    if (hasTile(road.get(r)[c])) {
                        type = TerrainType.BRIDGE;
                    }
                    grid.tiles[r][c] = new TerrainTile(r, c, type);
                }
            }
            return grid;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load layered terrain grid", e);
        }
    }

    private static List<String[]> readCsvResource(String resourcePath) throws Exception {
        try (InputStream in = TerrainGrid.class.getResourceAsStream(resourcePath)) {
            return readCsv(in, resourcePath);
        }
    }

    private static List<String[]> readCsv(InputStream in, String resourcePath) throws Exception {
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
        return lines;
    }

    private static void validateSameSize(List<String[]> base, List<String[]> candidate, String resourcePath) {
        if (candidate.size() != base.size()) {
            throw new IllegalArgumentException("Terrain csv row count mismatch: " + resourcePath);
        }

        int cols = base.get(0).length;
        for (int r = 0; r < candidate.size(); r++) {
            if (candidate.get(r).length != cols) {
                throw new IllegalArgumentException("Terrain csv column count mismatch at row " + r + ": " + resourcePath);
            }
        }
    }

    private static boolean hasTile(String code) {
        return !"-1".equals(code.trim());
    }

    private static boolean isMiddleOfCsvRun(List<String[]> layer, int row, int col, int allowedWidth) {
        if (!hasTile(layer.get(row)[col]) || allowedWidth <= 0) {
            return false;
        }

        int start = col;
        while (start - 1 >= 0 && hasTile(layer.get(row)[start - 1])) {
            start--;
        }

        int end = col;
        while (end + 1 < layer.get(row).length && hasTile(layer.get(row)[end + 1])) {
            end++;
        }

        int runWidth = end - start + 1;
        int laneWidth = Math.min(allowedWidth, runWidth);
        int laneStart = start + (int) Math.ceil((runWidth - laneWidth) / 2.0);
        int laneEnd = laneStart + laneWidth - 1;
        return col >= laneStart && col <= laneEnd;
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
            case "G":
            case "D":
                return TerrainType.BRIDGE;
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
        GridCoordinate coordinate = worldToGrid(worldPosition);
        int row = coordinate.getRow();
        int col = coordinate.getCol();
        if (!isInside(row, col)) {
            return TerrainType.ROCK;
        }
        return tiles[row][col].getType();
    }

    public boolean setTerrainAt(Vector2D worldPosition, TerrainType type) {
        if (worldPosition == null || type == null) {
            return false;
        }
        GridCoordinate coordinate = worldToGrid(worldPosition);
        int row = coordinate.getRow();
        int col = coordinate.getCol();
        if (!isInside(row, col)) {
            return false;
        }
        tiles[row][col].setType(type);
        return true;
    }

    public boolean isMiddleOfWaterRun(Vector2D worldPosition, int allowedWidth) {
        if (worldPosition == null || allowedWidth <= 0) {
            return false;
        }

        GridCoordinate coordinate = worldToGrid(worldPosition);
        int row = coordinate.getRow();
        int col = coordinate.getCol();
        if (!isInside(row, col) || tiles[row][col].getType() != TerrainType.WATER) {
            return false;
        }

        int start = col;
        while (start - 1 >= 0 && tiles[row][start - 1].getType() == TerrainType.WATER) {
            start--;
        }

        int end = col;
        while (end + 1 < cols && tiles[row][end + 1].getType() == TerrainType.WATER) {
            end++;
        }

        int runWidth = end - start + 1;
        int laneWidth = Math.min(allowedWidth, runWidth);
        int laneStart = start + (runWidth - laneWidth) / 2;
        int laneEnd = laneStart + laneWidth - 1;
        return col >= laneStart && col <= laneEnd;
    }

    public GridCoordinate worldToGrid(Vector2D worldPosition) {
        int col = (int) (worldPosition.x / tileSize);
        int row = (int) (worldPosition.y / tileSize);
        return new GridCoordinate(row, col);
    }

    public Vector2D gridToWorldCenter(int row, int col) {
        return new Vector2D(
            (col + 0.5) * tileSize,
            (row + 0.5) * tileSize
        );
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
