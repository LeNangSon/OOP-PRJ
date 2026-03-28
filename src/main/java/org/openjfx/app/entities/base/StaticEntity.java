package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;

public abstract class StaticEntity extends Entity {

    public StaticEntity(Vector2D position, double size, String shape) {
        super(position, size, shape);
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Vật thể tĩnh mặc định không làm gì
    }
    // Các lớp con (như Grass, Rock) tự định nghĩa hàm update().
    // Ví dụ: Grass có thể dùng update() để đếm thời gian mọc lại sau khi bị ăn.
}