package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Rabbit extends Herbivore {

    public Rabbit(Vector2D position){
        super(position, 40.0, "circle", 100.0, 1.0, 1.5);
        this.setRadius(40.0);
        this.type = EntityType.RABBIT;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    @Override
    public String toString() {
        // Giả sử file ảnh của bạn tên là Elephant.png (hoặc Elephant.jpg)
        // Lưu ý: Tên này phải khớp chính xác với file trong folder resources
        return "org/openjfx/app/Rabbit.png";
    }
}
