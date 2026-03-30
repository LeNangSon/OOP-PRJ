package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;

public class Rock extends Obstacle {

    public Rock(Vector2D position, double size, String shape) {
        super(position, 50, "Rock");
    }
    @Override
    public String toString() {
        // Giả sử file ảnh của bạn tên là Elephant.png (hoặc Elephant.jpg)
        // Lưu ý: Tên này phải khớp chính xác với file trong folder resources
        return "org/openjfx/app/Rock.png";
    }
}