package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.entities.base.Carnivore;

public class Wolf extends Carnivore {

    public Wolf(Vector2D position) {
        super(
                position,
                40.0,      // size
                "circle",  // shape
                200.0,     // initialHealth
                3.0,       // hungerRate
                0.0,       // thirstRate
                35.0,      // maxSpeed (giảm 40→35, thỏ vẫn chậm hơn nhưng có cơ hội trốn)
                70.0,      // maxForce
                3.0,       // mass
                30.0,      // wanderDistance
                30.0       // wanderRadius
        );

        this.setVisionRadius(160.0); // giảm 200→160, thỏ phát hiện sói trước
        this.type = EntityType.WOLF;
        
        // THAY ĐỔI Ở ĐÂY: Gán trực tiếp vào biến để tránh lỗi không tìm thấy method
        // Đảm bảo HunterStrategy được thực thi để bắn Log ra Terminal cho Nam
        this.moveStrategy = new HunterStrategy(); 
    }

    @Override
    public void update(double dt, WorldMap world) {
        // Logic của Carnivore sẽ tự động điều phối hành vi
        super.update(dt, world);
    }

    @Override
    public String toString() {
        return "org/openjfx/app/Wolf.png";
    }
}