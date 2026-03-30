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
        // Cách lấy tên Class sạch nhất (trả về "Rabbit", "Elephant"...)
        String className = entity.getClass().getSimpleName();

        try {
            // Phải thêm đuôi .png vào sau tên class
            String path = "/" + className + ".png";
            var stream = getClass().getResourceAsStream(path);

            if (stream != null) {
                Image img = new Image(stream);
                gc.drawImage(img, entity.getPosition().x, entity.getPosition().y, entity.getSize(), entity.getSize());
            } else {
                // Nếu không tìm thấy ảnh, vẽ tạm hình tròn để biết thực thể vẫn tồn tại
                gc.strokeOval(entity.getPosition().x, entity.getPosition().y, entity.getSize(), entity.getSize());
                System.out.println("Missing image: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}