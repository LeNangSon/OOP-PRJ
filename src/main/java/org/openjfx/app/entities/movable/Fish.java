package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Fish extends Herbivore {
    
    public Fish(Vector2D position){
        super(position, 50.0, "ellipse", 100.0, 0.5, 0.0);
        this.setRadius(30.0);
        this.type = EntityType.FISH;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
     @Override
    public String toString() {
        // Giả sử file ảnh của bạn tên là Elephant.png (hoặc Elephant.jpg)
        // Lưu ý: Tên này phải khớp chính xác với file trong folder resources
        return "org/openjfx/app/Fish.png";
    }
}
