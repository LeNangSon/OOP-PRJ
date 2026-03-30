package org.openjfx.app.environment;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Environment;
import org.openjfx.app.core.Vector2D;

public class Lake extends Environment {
    public Lake(Vector2D position, double width, double height) {
        super(position, width, height, "Lake");
        this.type = EntityType.WATER; 
    }
    @Override
    public String toString() {
        // Giả sử file ảnh của bạn tên là Elephant.png (hoặc Elephant.jpg)
        // Lưu ý: Tên này phải khớp chính xác với file trong folder resources
        return "org/openjfx/app/Lake.png";
    }
}