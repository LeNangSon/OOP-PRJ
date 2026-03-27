package org.openjfx.app.core;

import java.util.ArrayList;
import java.util.List;

import org.openjfx.app.entities.base.Entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class WorldMap {
    private final double width;
    private final double height;
    private final List<Entity> entities;

    public WorldMap(double width, double height) {
        this.width = width;
        this.height = height;
        this.entities = new ArrayList<>();//thêm các con vật và vật tĩnh
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
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
        //Vẽ nền Map
        gc.clearRect(0, 0, width, height); 
        //chèn ảnh vào tọa độ của vật
        for (Entity entity : entities) {
            renderEntityWithImage(gc, entity);
        }
    }
     // 4. Hàm "Radar" - Cung cấp tầm nhìn cho AI
    public List<Entity> getNeighbors(Entity owner, double radius) {
        
        return null; // Thay bằng list kết quả
    }
    // Xung đột
    private void handleBounds(Entity e) {
        
    }

    private void renderEntityWithImage(GraphicsContext gc, Entity entity) {
    // 1. Tách chuỗi từ toString() để lấy tên lớp
    String[] parts = entity.toString().split("\\{");
    String imagePath = parts[0]; 
    // 2. Load ảnh trực tiếp từ chuỗi đã tách
    Image img = new Image(getClass().getResourceAsStream("/" + imagePath));
    // 3. Vẽ ảnh lên Canvas
        gc.drawImage(img,entity.getPosition().x,entity.getPosition().y,entity.getSize(),entity.getSize());
}
}
