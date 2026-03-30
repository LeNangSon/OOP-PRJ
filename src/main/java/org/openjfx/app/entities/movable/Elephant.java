package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Herbivore;

public class Elephant extends Herbivore {

    public Elephant(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, 120.0, "rect", 100.0, 5.0, 6.0);
        this.setRadius(40.0);
        this.type = EntityType.ELEPHANT;
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
    @Override
    public String toString() {
        // Giả sử file ảnh của bạn tên là Elephant.png (hoặc Elephant.jpg)
        // Lưu ý: Tên này phải khớp chính xác với file trong folder resources
        return "org/openjfx/app/Elephant.png";
    }
}