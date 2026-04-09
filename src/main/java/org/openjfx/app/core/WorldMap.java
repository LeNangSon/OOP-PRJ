package org.openjfx.app.core;

import java.util.ArrayList;
import java.util.List;

import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.core.terrain.TerrainGrid;
import org.openjfx.app.core.terrain.TerrainType;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

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

    public TerrainType getTerrainAt(Vector2D position) {
        if (terrainGrid == null) {
            return TerrainType.LAND;
        }
        return terrainGrid.getTerrainAt(position);
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
    // 1. Vẽ map nền (nếu có ảnh thì dùng ảnh, không thì vẽ LAND bằng code).
    if (fixedBackgroundImage != null) {
        gc.drawImage(fixedBackgroundImage, 0, 0, width, height);
    } else {
        drawGrassBackground(gc);
    }

    // 2. Vẽ các thực thể (Thỏ, Sói...) đè lên map.
    for (Entity entity : entities) {
        renderEntityWithImage(gc, entity);
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