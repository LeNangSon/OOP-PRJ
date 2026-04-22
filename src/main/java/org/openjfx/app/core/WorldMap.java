package org.openjfx.app.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List; // Thêm để xóa thực thể an toàn

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
    private final List<GameObserver> observers = new ArrayList<>(); // Observer list

    public WorldMap(double width, double height) {
        this.width = width;
        this.height = height;
        this.entities = new ArrayList<>();
    }

    // --- CÁC HÀM CHO OBSERVER (Terminal Log) ---

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    /**
     * Hàm này để LivingEntity gọi khi máu về 0
     */
    public void broadcastDeath(String message) {
        for (GameObserver obs : observers) {
            obs.onEntityDeath(message);
        }
    }

    public void notifyAction(String actor, String action, String target) {
        for (GameObserver obs : observers) {
            obs.onActionOccurred(actor, action, target);
        }
    }

    // --- LOGIC CẬP NHẬT ---

    public void update(double dt) {
        // Sử dụng Iterator để có thể xóa thực thể đã chết khỏi danh sách khi update
        Iterator<Entity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            entity.update(dt, this);

            // Nếu là thực thể sống và đã chết, có thể chọn xóa khỏi list sau một khoảng thời gian
            // Hoặc để đơn giản, nếu isAlive() == false thì bạn có thể remove ở đây
            /*
            if (entity instanceof LivingEntity && !((LivingEntity) entity).isAlive()) {
                // iterator.remove(); 
            }
            */
        }
    }

    // --- CÁC HÀM HỖ TRỢ ĐỊA HÌNH & THỰC THỂ (Giữ nguyên logic của bạn) ---

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

    public boolean canStandOn(LivingEntity entity, Vector2D position) {
        TerrainType terrain = getTerrainAt(position);
        EntityType entityType = entity.getType();
        if (terrain == TerrainType.WATER) return entityType == EntityType.FISH;
        if (terrain == TerrainType.PIT) return false;
        return terrain != TerrainType.ROCK;
    }

    public Entity getEntityById(int id) {
        for (Entity e : entities) {
            if (e.getId() == id) return e;
        }
        return null;
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

    // --- RENDER (Giữ nguyên ý tưởng của bạn) ---

    public void render(GraphicsContext gc) {
        if (fixedBackgroundImage != null) {
            gc.drawImage(fixedBackgroundImage, 0, 0, width, height);
        } else {
            drawGrassBackground(gc);
        }

        for (Entity entity : entities) {
            // Chỉ vẽ những con còn sống (Hoặc vẽ xác nếu muốn)
            if (entity instanceof LivingEntity && !((LivingEntity) entity).isAlive()) {
                // Bạn có thể vẽ một cái mộ hoặc làm mờ ảnh tại đây
                gc.setGlobalAlpha(0.3); // Làm mờ xác chết
                renderEntityWithImage(gc, entity);
                gc.setGlobalAlpha(1.0);
            } else {
                renderEntityWithImage(gc, entity);
                renderWanderDebug(gc, entity);
            }
        }
    }

    private void renderEntityWithImage(GraphicsContext gc, Entity entity) {
        String[] parts = entity.toString().split("\\{");
        String imagePath = parts[0]; 
        try {
            Image img = new Image(getClass().getResourceAsStream("/" + imagePath));
            double renderX = entity.getPosition().x - (entity.getSize() / 2);
            double renderY = entity.getPosition().y - (entity.getSize() / 2);
            gc.drawImage(img, renderX, renderY, entity.getSize(), entity.getSize());
        } catch (Exception e) {
            gc.setFill(Color.RED);
            gc.fillOval(entity.getPosition().x - 5, entity.getPosition().y - 5, 10, 10);
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
        gc.restore();
    }

    private void drawGrassBackground(GraphicsContext gc) {
        int tileSize = 40; 
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                gc.setFill((x / tileSize + y / tileSize) % 2 == 0 ? Color.web("#90EE90") : Color.web("#85e085"));
                gc.fillRect(x, y, tileSize, tileSize);
            }
        }
    }

    public void setFixedBackgroundImageFromResource(String resourcePath) {
        try {
            Image image = new Image(getClass().getResourceAsStream(resourcePath));
            fixedBackgroundImage = image.isError() ? null : image;
        } catch (Exception e) { fixedBackgroundImage = null; }
    }
}