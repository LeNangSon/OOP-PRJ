package org.openjfx.app.core;

import java.util.*;

import org.openjfx.app.core.strategies.WanderStrategy;
import org.openjfx.app.core.terrain.TerrainGrid;
import org.openjfx.app.core.terrain.TerrainTile;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class WorldMap {
    private final double width;
    private final double height;
    private final List<Entity> entities;
    private TerrainGrid terrainGrid;
    private Image fixedBackgroundImage;
    public WorldMap(double width, double height) {
        this.width = width;
        this.height = height;
        this.entities = new ArrayList<>();//thêm các con vật và vật tĩnh
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void setTerrainGrid(TerrainGrid terrainGrid) {
        this.terrainGrid = terrainGrid;
    }

    public void setTerrainGridFromCsvResource(String resourcePath, int tileSize) {
        this.terrainGrid = TerrainGrid.fromCsvResource(resourcePath, tileSize);
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public TerrainType getTerrainAt(Vector2D position) {
        if (terrainGrid == null) {
            return TerrainType.LAND;
        }
        return terrainGrid.getTerrainAt(position);
    }


    // Khởi tạo các node
    private class AstarNode implements Comparable<AstarNode>{
        int row, col;
        double g;
        double h;                  // heuristic
        AstarNode parent;

        // constructor
        public AstarNode(int row, int col){
            this.row = row;
            this.col = col;
            this.g = Double.MAX_VALUE;
        }

        public double getF(){
            return g+h;
        }


        // Chỉ dẫn cho priority queue hoạt động
        @Override
        public int compareTo(AstarNode other) {
            return Double.compare(this.getF(), other.getF());
        }
    }

    public TerrainGrid.GridCoordinate worldToGrid(Vector2D position) {
        if (terrainGrid == null || position == null) {
            return null;
        }
        return terrainGrid.worldToGrid(position);
    }

    public Vector2D gridToWorldCenter(int row, int col) {
        if (terrainGrid == null || !terrainGrid.isInside(row, col)) {
            return null;
        }
        return terrainGrid.gridToWorldCenter(row, col);
    }

    public List<Vector2D> findPathAStar(LivingEntity entity, Vector2D start, Vector2D target){
        TerrainGrid.GridCoordinate start_grid = worldToGrid(start);
        TerrainGrid.GridCoordinate target_grid = worldToGrid(target);

        // rows, cols của map
        int rows = terrainGrid.getRows();
        int cols = terrainGrid.getCols();

        int s_row = start_grid.getRow();
        int s_col = start_grid.getCol();
        int t_row = target_grid.getRow();
        int t_col = target_grid.getCol();

        PriorityQueue<AstarNode> openSet = new PriorityQueue<>();
        boolean[][] visited = new boolean[rows][cols];
        AstarNode[][] allNodes = new AstarNode[rows][cols];

        for(int r = 0; r < rows; r++){
            for(int c = 0; c < cols;c++){
                allNodes[r][c] = new AstarNode(r,c);
            }
        }

        AstarNode startNode = allNodes[s_row][s_col];
        AstarNode targetNode = allNodes[t_row][t_col];

        startNode.g = 0;
        startNode.h = calculateHeuristic(startNode, targetNode);
        openSet.add(startNode);

        int[][] directions = {
                {-1,0},{1,0},{0,-1},{0,1},
                {-1,-1},{-1,1},{1,-1},{1,1}
        };

        while(!openSet.isEmpty()){
            AstarNode current = openSet.poll();

            // Nếu tìm thấy node target
            if(current.row == t_row && current.col == t_col){
                return Path(current);
            }

            visited[current.row][current.col] = true;
            for(int[] dir: directions){
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];

                // Valiation
                // Ngoài map
                if(newRow < 0 || newCol < 0 || newRow > rows || newCol > cols) continue;
                if(visited[newRow][newCol]) continue;

                // Check đi vào được không
                Vector2D titleCenter = gridToWorldCenter(newRow, newCol);
                if(!canStandOn(entity, titleCenter)) continue;

                AstarNode nextNode = allNodes[newRow][newCol];

                double moveCost =  (dir[0] != 0 && dir[1] != 0) ? Math.sqrt(2) : 1.0;
                double tmpMoveCost = current.g + moveCost;

                if(tmpMoveCost < nextNode.g){
                    nextNode.parent = current;
                    nextNode.g = tmpMoveCost;
                    nextNode.h = calculateHeuristic(nextNode, targetNode);

                    // nếu nextNode chưa có trong p_queue --> thêm vào
                    if(!openSet.contains(nextNode)) {
                        openSet.add(nextNode);
                    // nếu nextNode đã có trong p_queue --> xoá cái cũ thêm vào cái mới tìm được
                    } else {
                        openSet.remove(nextNode);
                        openSet.add(nextNode);
                    }
                }
            }
        }
        return null;
    }

    private List<Vector2D> Path(AstarNode node){
        List<Vector2D> path = new ArrayList<>();
        AstarNode current = node;

        while(current != null){
            Vector2D currentPos = gridToWorldCenter(current.row, current.col);
            double X = currentPos.x;
            double Y = currentPos.y;

            path.add(new Vector2D(X,Y));
            current = current.parent;
        }
        // Đảo ngược đường để tìm start -> target
        Collections.reverse(path);

        // Bỏ vị trí hiện tại của entity
        if(!path.isEmpty()){
            path.remove(0);
        }
        return path;
    }

    private double calculateHeuristic(AstarNode start, AstarNode target){
        double s_row = start.row;
        double s_col = start.col;
        double t_row = target.row;
        double t_col = target.col;
        double dx = s_col - t_col;
        double dy = s_row - t_row;

        return Math.sqrt(dx*dx + dy*dy);
    }
    public Vector2D findNearestTerrainPosition(Vector2D from, TerrainType targetType) {
        if (terrainGrid == null || from == null || targetType == null) {
            return null;
        }

        Vector2D nearestCenter = null;
        double minDistance = Double.MAX_VALUE;

        int tileSize = terrainGrid.getTileSize();
        for (int row = 0; row < terrainGrid.getRows(); row++) {
            for (int col = 0; col < terrainGrid.getCols(); col++) {
                TerrainTile tile = terrainGrid.getTile(row, col);
                if (tile == null || tile.getType() != targetType) {
                    continue;
                }

                Vector2D center = new Vector2D(
                    (col + 0.5) * tileSize,
                    (row + 0.5) * tileSize
                );
                double distance = from.distance(center);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCenter = center;
                }
            }
        }

        return nearestCenter;
    }

    public boolean canStandOn(LivingEntity entity, Vector2D position) {
        TerrainType terrain = getTerrainAt(position);
        EntityType entityType = entity.getType();

        if (terrain == TerrainType.WATER) {
            return entityType == EntityType.FISH;
        }
        if (terrain == TerrainType.PIT) {
            return false;
        }
        return terrain != TerrainType.ROCK;
    }

    public void update(double dt) {
        for (Entity entity : entities) {
            entity.update(dt, this);
        }
    }

    public Entity getEntityById(int id) {
        for (Entity e : entities) {
            if (e.getId() == id) return e;
        }
        return null;
    }

    public void render(GraphicsContext gc) {
    // 1. Vẽ map nền (nếu có ảnh thì dùng ảnh, không thì vẽ cỏ mặc định).
    if (fixedBackgroundImage != null) {
        gc.drawImage(fixedBackgroundImage, 0, 0, width, height);
    } else {
        drawGrassBackground(gc);
    }

    // 2. Vẽ các thực thể (Thỏ, Sói...) đè lên map.
    for (Entity entity : entities) {
        renderEntityWithImage(gc, entity);
        renderWanderDebug(gc, entity);
    }
}

    public void setFixedBackgroundImageFromResource(String resourcePath) {
        try {
            Image image = new Image(getClass().getResourceAsStream(resourcePath));
            fixedBackgroundImage = image.isError() ? null : image;
        } catch (Exception e) {
            fixedBackgroundImage = null;
        }
    }

    public void setFixedBackgroundImageFromFile(String absoluteFilePath) {
        try {
            Image image = new Image("file:" + absoluteFilePath);
            fixedBackgroundImage = image.isError() ? null : image;
        } catch (Exception e) {
            fixedBackgroundImage = null;
        }
    }
     //Hàm "Radar" - Cung cấp tầm nhìn cho AI
    public List<Entity> getNeighbors(Entity owner, double radius) {
        List<Entity> result = new ArrayList<>();
        for (Entity e : entities) {
            if (e != owner) {
                double dist = owner.getPosition().distance(e.getPosition());
                if (dist <= radius) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    private void renderEntityWithImage(GraphicsContext gc, Entity entity) {
    // 1. Tách chuỗi từ toString() để lấy tên lớp/đường dẫn ảnh (Giữ nguyên ý Tuấn)
    String[] parts = entity.toString().split("\\{");
    String imagePath = parts[0]; 

    try {
        // 2. Load ảnh trực tiếp từ chuỗi đã tách (Không cộng thêm ".png")
        Image img = new Image(getClass().getResourceAsStream("/" + imagePath));

        // 3. TÍNH TOÁN ĐỂ KHỚP VỊ TRÍ VÀ KÍCH THƯỚC
        // Lấy tọa độ x, y làm tâm, trừ đi một nửa size để ảnh không bị lệch
        double renderX = entity.getPosition().x - (entity.getSize() / 2);
        double renderY = entity.getPosition().y - (entity.getSize() / 2);

        // Vẽ ảnh: khớp tuyệt đối với biến 'size' trong logic của Tuấn
        gc.drawImage(img, renderX, renderY, entity.getSize(), entity.getSize());
        
    } catch (Exception e) {
        // Nếu lỗi (sai imagePath hoặc thiếu file), vẽ hình tạm để tránh trắng màn hình
        gc.setFill(javafx.scene.paint.Color.RED);
        gc.fillOval(entity.getPosition().x - 5, entity.getPosition().y - 5, 10, 10);
        //System.err.println("Lỗi load ảnh: /" + imagePath);
    }
}

    private void renderWanderDebug(GraphicsContext gc, Entity entity) {
        WanderStrategy.DebugWanderState debugState = WanderStrategy.getDebugState(entity.getId());
        if (debugState == null) {
            return;
        }

        Vector2D center = debugState.getCircleCenter();
        Vector2D randomPoint = debugState.getRandomPoint();
        double radius = debugState.getWanderRadius();

        gc.save();
        gc.setLineWidth(1.5);

        gc.setStroke(Color.ORANGE);
        gc.strokeOval(center.x - radius, center.y - radius, radius * 2, radius * 2);

        gc.setStroke(Color.YELLOW);
        gc.strokeOval(randomPoint.x - 5, randomPoint.y - 5, 10, 10);
        gc.setFill(Color.YELLOW);
        gc.fillOval(randomPoint.x - 2, randomPoint.y - 2, 4, 4);
        gc.restore();
    }

    private void drawGrassBackground(GraphicsContext gc) {
    // Định nghĩa kích thước mỗi ô cỏ (ví dụ 40x40 pixel)
    int tileSize = 40; 

    for (int x = 0; x < width; x += tileSize) {
        for (int y = 0; y < height; y += tileSize) {
            // Tạo hiệu ứng bàn cờ nhẹ cho cỏ nhìn cho thật
            if ((x / tileSize + y / tileSize) % 2 == 0) {
                gc.setFill(javafx.scene.paint.Color.web("#90EE90")); // Xanh lá nhạt
            } else {
                gc.setFill(javafx.scene.paint.Color.web("#85e085")); // Xanh lá đậm hơn chút
            }
            
            // Vẽ ô cỏ tại tọa độ (x, y)
            gc.fillRect(x, y, tileSize, tileSize);

            // Vẽ thêm vài đốm cỏ nhỏ trang trí (tùy chọn)
            gc.setFill(javafx.scene.paint.Color.web("#77cc77"));
            gc.fillOval(x + 10, y + 10, 2, 2);
        }
    }
}

}