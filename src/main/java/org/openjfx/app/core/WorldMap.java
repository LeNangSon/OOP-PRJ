package org.openjfx.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
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
    private final List<GameObserver> observers = new ArrayList<>();

    // --- PHẦN THÊM MỚI: Kho chứa ảnh để tránh lag máy ---
    private final Map<String, Image> imageCache = new HashMap<>();

    public WorldMap(double width, double height) {
        this.width = width;
        this.height = height;
        this.entities = new ArrayList<>();
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

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    public TerrainType getTerrainAt(Vector2D position) {
        if (terrainGrid == null) return TerrainType.LAND;
        return terrainGrid.getTerrainAt(position);
    }

    public boolean setTerrainAt(Vector2D position, TerrainType type) {
        if (terrainGrid == null || position == null || type == null) {
            return false;
        }
        return terrainGrid.setTerrainAt(position, type);
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
        if (terrainGrid == null || position == null) return null;
        return terrainGrid.worldToGrid(position);
    }

    public Vector2D gridToWorldCenter(int row, int col) {
        if (terrainGrid == null || !terrainGrid.isInside(row, col)) return null;
        return terrainGrid.gridToWorldCenter(row, col);
    }

    public List<Vector2D> findPathAStar(LivingEntity entity, Vector2D start, Vector2D target){
        return findPathAStar(entity, start, target, null);
    }

    public List<Vector2D> findPathAStar(LivingEntity entity, Vector2D start, Vector2D target, Set<String> avoidedGridKeys){
        if (terrainGrid == null || entity == null || start == null || target == null) {
            return null;
        }

        TerrainGrid.GridCoordinate start_grid = worldToGrid(start);
        TerrainGrid.GridCoordinate target_grid = worldToGrid(target);
        if (start_grid == null || target_grid == null) {
            return null;
        }

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
                if(newRow < 0 || newCol < 0 || newRow >= rows || newCol >= cols) continue;
                if(visited[newRow][newCol]) continue;

                // Check đi vào được không
                Vector2D titleCenter = gridToWorldCenter(newRow, newCol);
                if (titleCenter == null) continue;
                if(!canStandOn(entity, titleCenter)) continue;
                if (avoidedGridKeys != null && avoidedGridKeys.contains(gridKey(newRow, newCol))) continue;

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

    private String gridKey(int row, int col) {
        return row + ":" + col;
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
        if (terrainGrid == null || from == null || targetType == null) return null;
        Vector2D nearestCenter = null;
        double minDistance = Double.MAX_VALUE;
        int tileSize = terrainGrid.getTileSize();
        for (int row = 0; row < terrainGrid.getRows(); row++) {
            for (int col = 0; col < terrainGrid.getCols(); col++) {
                TerrainTile tile = terrainGrid.getTile(row, col);
                if (tile == null || tile.getType() != targetType) continue;
                Vector2D center = new Vector2D((col + 0.5) * tileSize, (row + 0.5) * tileSize);
                double distance = from.distance(center);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestCenter = center;
                }
            }
        }
        return nearestCenter;
    }

    public Vector2D findNearestTerrainPositionInRadius(Vector2D from, TerrainType targetType, double radius) {
        if (terrainGrid == null || from == null || targetType == null || radius <= 0) return null;
        TerrainGrid.GridCoordinate centerCoordinate = worldToGrid(from);
        if (centerCoordinate == null) return null;
        int tileSize = terrainGrid.getTileSize();
        int tileRadius = (int) Math.ceil(radius / tileSize);
        int centerRow = centerCoordinate.getRow();
        int centerCol = centerCoordinate.getCol();
        Vector2D nearestCenter = null;
        double minDistanceSquared = Double.MAX_VALUE;
        double radiusSquared = radius * radius;
        for (int row = centerRow - tileRadius; row <= centerRow + tileRadius; row++) {
            for (int col = centerCol - tileRadius; col <= centerCol + tileRadius; col++) {
                if (!terrainGrid.isInside(row, col)) continue;
                TerrainTile tile = terrainGrid.getTile(row, col);
                if (tile == null || tile.getType() != targetType) continue;
                Vector2D center = terrainGrid.gridToWorldCenter(row, col);
                double dx = center.x - from.x;
                double dy = center.y - from.y;
                double distanceSquared = dx * dx + dy * dy;
                if (distanceSquared > radiusSquared) continue;
                if (distanceSquared < minDistanceSquared) {
                    minDistanceSquared = distanceSquared;
                    nearestCenter = center;
                }
            }
        }
        return nearestCenter;
    }

    public Vector2D findFarthestTerrainPositionFromThreat(
            Vector2D from,
            Vector2D threatPos,
            TerrainType targetType,
            double searchRadius,
            LivingEntity entity) {
        if (terrainGrid == null || from == null || threatPos == null || targetType == null || searchRadius <= 0) {
            return null;
        }
        TerrainGrid.GridCoordinate centerCoordinate = worldToGrid(from);
        if (centerCoordinate == null) {
            return null;
        }
        int tileSize = terrainGrid.getTileSize();
        int tileRadius = (int) Math.ceil(searchRadius / tileSize);
        int centerRow = centerCoordinate.getRow();
        int centerCol = centerCoordinate.getCol();
        Vector2D best = null;
        double maxDistFromThreatSq = -1.0;
        double searchRadiusSq = searchRadius * searchRadius;
        for (int row = centerRow - tileRadius; row <= centerRow + tileRadius; row++) {
            for (int col = centerCol - tileRadius; col <= centerCol + tileRadius; col++) {
                if (!terrainGrid.isInside(row, col)) {
                    continue;
                }
                TerrainTile tile = terrainGrid.getTile(row, col);
                if (tile == null || tile.getType() != targetType) {
                    continue;
                }
                Vector2D center = terrainGrid.gridToWorldCenter(row, col);
                double dxFrom = center.x - from.x;
                double dyFrom = center.y - from.y;
                if (dxFrom * dxFrom + dyFrom * dyFrom > searchRadiusSq) {
                    continue;
                }
                if (!canStandOn(entity, center)) {
                    continue;
                }
                double dxThreat = center.x - threatPos.x;
                double dyThreat = center.y - threatPos.y;
                double distFromThreatSq = dxThreat * dxThreat + dyThreat * dyThreat;
                if (distFromThreatSq > maxDistFromThreatSq) {
                    maxDistFromThreatSq = distFromThreatSq;
                    best = center;
                }
            }
        }
        return best;
    }

    // Tìm ô an toàn nhất so với DANH SÁCH threats: max của (min khoảng cách tới mọi threat).
    // Khác findFarthestTerrainPositionFromThreat: hàm đó chỉ xét 1 threat, có thể chọn ô xa centroid
    // nhưng vẫn sát một threat cụ thể.
    public Vector2D findSafestTerrainPosition(
            Vector2D from,
            List<Vector2D> threatPositions,
            TerrainType targetType,
            double searchRadius,
            LivingEntity entity) {
        if (terrainGrid == null || from == null || threatPositions == null || threatPositions.isEmpty()
                || targetType == null || searchRadius <= 0) {
            return null;
        }
        TerrainGrid.GridCoordinate centerCoordinate = worldToGrid(from);
        if (centerCoordinate == null) {
            return null;
        }
        int tileSize = terrainGrid.getTileSize();
        int tileRadius = (int) Math.ceil(searchRadius / tileSize);
        int centerRow = centerCoordinate.getRow();
        int centerCol = centerCoordinate.getCol();
        Vector2D best = null;
        double bestScore = -1.0;
        double searchRadiusSq = searchRadius * searchRadius;
        for (int row = centerRow - tileRadius; row <= centerRow + tileRadius; row++) {
            for (int col = centerCol - tileRadius; col <= centerCol + tileRadius; col++) {
                if (!terrainGrid.isInside(row, col)) continue;
                TerrainTile tile = terrainGrid.getTile(row, col);
                if (tile == null || tile.getType() != targetType) continue;
                Vector2D center = terrainGrid.gridToWorldCenter(row, col);
                double dxFrom = center.x - from.x;
                double dyFrom = center.y - from.y;
                if (dxFrom * dxFrom + dyFrom * dyFrom > searchRadiusSq) continue;
                if (!canStandOn(entity, center)) continue;

                double minThreatDistSq = Double.MAX_VALUE;
                for (Vector2D threatPos : threatPositions) {
                    if (threatPos == null) continue;
                    double dxT = center.x - threatPos.x;
                    double dyT = center.y - threatPos.y;
                    double dSq = dxT * dxT + dyT * dyT;
                    if (dSq < minThreatDistSq) minThreatDistSq = dSq;
                }

                if (minThreatDistSq > bestScore) {
                    bestScore = minThreatDistSq;
                    best = center;
                }
            }
        }
        return best;
    }

    public boolean canStandOn(LivingEntity entity, Vector2D position) {
        TerrainType terrain = getTerrainAt(position);
        EntityType entityType = entity.getType();
        if (terrain == TerrainType.WATER) return entityType == EntityType.FISH;
        if (terrain == TerrainType.BUSH) {
            return entityType == EntityType.RABBIT;
        }
        if (terrain == TerrainType.PIT) return false;
        return terrain != TerrainType.ROCK;
    }

    // --- CẬP NHẬT: Duyệt và xóa thực thể đã chết để giải phóng Log ---
    public void update(double dt) {
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity e = entities.get(i);
            e.update(dt, this);

            // Nếu thực thể sống đã chết, xóa khỏi danh sách để Terminal không bị rác
            if (e instanceof LivingEntity && !((LivingEntity) e).isAlive()) {
                entities.remove(i);
            }
        }
    }

    public Entity getEntityById(int id) {
        for (Entity e : entities) {
            if (e.getId() == id) return e;
        }
        return null;
    }
    
    public List<Entity> getEntities() {
    return Collections.unmodifiableList(entities);
}

    public void render(GraphicsContext gc) {
        // 1. Lưu trạng thái ban đầu của GraphicsContext
        gc.save(); 
        gc.translate(offsetX, offsetY);
        // 2. Thực hiện phóng to/thu nhỏ dựa trên biến scale
        gc.scale(scale, scale);

        // --- GIỮ NGUYÊN LOGIC VẼ CỦA BẠN ---
        if (fixedBackgroundImage != null) {
            gc.drawImage(fixedBackgroundImage, 0, 0, width, height);
        } else {
            drawGrassBackground(gc);
        }

        for (Entity entity : entities) {
            renderVisionRadius(gc, entity);
            renderEntityWithImage(gc, entity);
            renderWanderDebug(gc, entity);
            renderAStarPathDebug(gc, entity);
        }
        // ----------------------------------

        // 3. Khôi phục lại trạng thái ban đầu (để tránh làm hỏng các phần vẽ khác bên ngoài WorldMap)
        gc.restore(); 
    }

    private void renderVisionRadius(GraphicsContext gc, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity livingEntity = (LivingEntity) entity;
        double visionRadius = livingEntity.getVisionRadius();
        if (visionRadius <= 0) {
            return;
        }

        gc.save();
        gc.setLineWidth(1.2);
        gc.setStroke(Color.web("#66E0FF", 0.55));
        gc.setFill(Color.web("#66E0FF", 0.08));

        double diameter = visionRadius * 2;
        double topLeftX = livingEntity.getPosition().x - visionRadius;
        double topLeftY = livingEntity.getPosition().y - visionRadius;

        gc.fillOval(topLeftX, topLeftY, diameter, diameter);
        gc.strokeOval(topLeftX, topLeftY, diameter, diameter);
        gc.restore();
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

    public List<Entity> getNeighbors(Entity owner, double radius) {
        List<Entity> result = new ArrayList<>();
        for (Entity e : entities) {
            if (e != owner) {
                double dist = owner.getPosition().distance(e.getPosition());
                if (dist <= radius) result.add(e);
            }
        }
        return result;
    }


    // --- CẬP NHẬT: Render dùng ImageCache để mượt hơn ---
    private void renderEntityWithImage(GraphicsContext gc, Entity entity) {
        String[] parts = entity.toString().split("\\{");
        String imagePath = parts[0]; 

        try {
            // Lấy ảnh từ cache, nếu chưa có thì mới load từ resource
            Image img = imageCache.get(imagePath);
            if (img == null) {
                img = new Image(getClass().getResourceAsStream("/" + imagePath));
                imageCache.put(imagePath, img);
            }

            double aspect = img.getWidth() / img.getHeight();
            double drawH = entity.getSize();
            double drawW = drawH * aspect;
            double renderX = entity.getPosition().x - drawW / 2;
            double renderY = entity.getPosition().y - drawH / 2;
            gc.drawImage(img, renderX, renderY, drawW, drawH);
            
        } catch (Exception e) {
            gc.setFill(Color.RED);
            gc.fillOval(entity.getPosition().x - 5, entity.getPosition().y - 5, 10, 10);
        }
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    public void notifyAction(String actor, String action, String target) {
        for (GameObserver obs : observers) {
            obs.onActionOccurred(actor, action, target);
        }
    }

    public void broadcastDeath(String message) {
        for (GameObserver obs : observers) {
            obs.onEntityDeath(message);
        }
    }

    private void renderWanderDebug(GraphicsContext gc, Entity entity) {
        WanderStrategy.DebugWanderState debugState = WanderStrategy.getDebugState(entity.getId());
        if (debugState == null) return;
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

    private void renderAStarPathDebug(GraphicsContext gc, Entity entity) {
        List<Vector2D> path = null;
        HunterStrategy.DebugPathState hunterPath = HunterStrategy.getDebugPathState(entity.getId());
        if (hunterPath != null) {
            path = hunterPath.getPath();
        } else {
            FleeStrategy.DebugPathState fleePath = FleeStrategy.getDebugPathState(entity.getId());
            if (fleePath != null) {
                path = fleePath.getPath();
            }
        }

        if (path == null || path.size() < 2) {
            return;
        }

        gc.save();
        gc.setLineWidth(2.0);
        gc.setStroke(Color.web("#00D4FF", 0.85));
        gc.setFill(Color.web("#00D4FF", 0.85));

        Vector2D previous = null;
        for (Vector2D point : path) {
            if (point == null) {
                continue;
            }
            if (previous != null) {
                gc.strokeLine(previous.x, previous.y, point.x, point.y);
            }
            gc.fillOval(point.x - 2.5, point.y - 2.5, 5, 5);
            previous = point;
        }

        gc.restore();
    }

    private void drawGrassBackground(GraphicsContext gc) {
        int tileSize = 40; 
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                if ((x / tileSize + y / tileSize) % 2 == 0) {
                    gc.setFill(Color.web("#90EE90"));
                } else {
                    gc.setFill(Color.web("#85e085"));
                }
                gc.fillRect(x, y, tileSize, tileSize);
                gc.setFill(Color.web("#77cc77"));
                gc.fillOval(x + 10, y + 10, 2, 2);
            }
        }
    }
    private double scale = 1.0;
    // Thêm getter/setter để MainApp có thể gọi
    public void setScale(double scale) {
    this.scale = Math.max(1.0, Math.min(3.0, scale));
    }
    public double getScale() { return scale; }
    private double offsetX = 0;
    private double offsetY = 0;

    // Getter và Setter cho Offset
    public void setOffset(double x, double y) {
    double scaledWidth = this.width * this.scale;
    double scaledHeight = this.height * this.scale;

    // Chặn biên X: Nếu bản đồ to hơn khung hình thì mới cho lia
    if (scaledWidth > this.width) {
        this.offsetX = Math.min(0, Math.max(x, this.width - scaledWidth));
    } else {
        // Nếu không muốn đứng yên tại 0, bạn có thể gán this.offsetX = x;
        // Nhưng theo ý bạn là "bản đồ cố định" nên để là 0 hoặc căn giữa:
        this.offsetX = (this.width - scaledWidth) / 2; 
    }

    // Chặn biên Y
    if (scaledHeight > this.height) {
        this.offsetY = Math.min(0, Math.max(y, this.height - scaledHeight));
    } else {
        this.offsetY = (this.height - scaledHeight) / 2;
    }
    }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
}