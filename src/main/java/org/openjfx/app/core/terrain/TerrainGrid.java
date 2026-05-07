package org.openjfx.app.core.terrain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.openjfx.app.core.Vector2D;

public class TerrainGrid {
    private final int rows = 80;
    private final int cols = 80;
    private final TerrainTile[][] grid;
    private final int tileSize;

    public TerrainGrid(int tileSize) {
        this.tileSize = tileSize;
        this.grid = new TerrainTile[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Mặc định ban đầu là ô trống (ID -1)
                grid[r][c] = new TerrainTile(c, r, TerrainType.LAND, -1);
            }
        }
    }
    /**
 * Cập nhật một ô tile tại vị trí (row, col).
 */
public void setTile(int row, int col, TerrainTile tile) {
    if (row >= 0 && row < rows && col >= 0 && col < cols) {
        grid[row][col] = tile;
    }
}
public TerrainTile[][] getGrid() {
    return this.grid;
}

    public void loadLayerFromCSV(String path, TerrainType type) {
    int[][] overlayData = loadRawDataFromCsv(path); 
    
    for (int r = 0; r < rows; r++) {
        for (int c = 0; c < cols; c++) {
            int id = overlayData[r][c];
            
            if (id > 0) { // Nếu ô có ID (không phải ô trống)
                if (grid[r][c] == null) {
                    grid[r][c] = new TerrainTile(c, r, type, id);
                } else {
                    grid[r][c].setTileId(id);
                    grid[r][c].setType(type); // Gán loại địa hình của Layer này
                }
            }
        }
    }
}
private int[][] loadRawDataFromCsv(String path) {
    int[][] data = new int[rows][cols];
    
    // Khởi tạo mảng với giá trị -1 để biết chỗ nào chưa có dữ liệu
    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) data[i][j] = -1;
    }

    try (InputStream is = getClass().getResourceAsStream(path)) {
        if (is == null) {
            // CỰC KỲ QUAN TRỌNG: In ra để biết đường dẫn nào đang bị sai
            System.err.println("LỖI: Không tìm thấy file CSV tại: " + path);
            return data;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            int r = 0;
            while ((line = br.readLine()) != null && r < rows) {
                // Xử lý dòng trống nếu có
                if (line.trim().isEmpty()) continue;

                String[] values = line.split(",");
                for (int c = 0; c < values.length && c < cols; c++) {
                    try {
                        String val = values[c].trim();
                        if (!val.isEmpty()) {
                            data[r][c] = Integer.parseInt(val);
                        }
                    } catch (NumberFormatException e) {
                        data[r][c] = -1; // Nếu không phải số thì bỏ qua
                    }
                }
                r++;
            }
        }
    } catch (Exception e) {
        System.err.println("Lỗi đọc dữ liệu CSV: " + e.getMessage());
    }
    return data;
}
    // --- CÁC HÀM HỖ TRỢ WORLDMAP ---[cite: 45]
    
    public GridCoordinate worldToGrid(Vector2D position) {
        if (position == null) return null;
        int c = (int) (position.x / tileSize);
        int r = (int) (position.y / tileSize);
        return isInside(r, c) ? new GridCoordinate(r, c) : null;
    }

    public Vector2D gridToWorldCenter(int row, int col) {
        if (!isInside(row, col)) return null;
        return new Vector2D((col + 0.5) * tileSize, (row + 0.5) * tileSize);
    }

    public TerrainType getTerrainAt(Vector2D pos) {
        GridCoordinate coord = worldToGrid(pos);
        return (coord != null) ? grid[coord.getRow()][coord.getCol()].getType() : TerrainType.OBSTACLE;
    }

    public boolean isInside(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public TerrainTile getTile(int r, int c) {
        return isInside(r, c) ? grid[r][c] : null;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTileSize() { return tileSize; }

    // Dùng cho WorldMap.setTerrainGridFromCsvResource[cite: 46]
    // Tìm đến cuối file TerrainGrid.java và sửa lại hàm này:
public static TerrainGrid fromCsvResource(String path, int tileSize, TerrainType type) {
    TerrainGrid tg = new TerrainGrid(tileSize);
    tg.loadLayerFromCSV(path, type); // Phải gọi hàm loadLayer có kèm type
    return tg;
}

    // Lớp tọa độ hỗ trợ[cite: 45]
    public static class GridCoordinate {
        private final int row, col;
        public GridCoordinate(int r, int c) { this.row = r; this.col = c; }
        public int getRow() { return row; }
        public int getCol() { return col; }
    }
}