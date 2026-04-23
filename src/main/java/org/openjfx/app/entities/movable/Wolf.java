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
                100.0,     // initialHealth
                5.0,       // hungerRate
                0.0,       // thirstRate
                30.0,      // maxSpeed
                50.0,       // maxForce
                3.0,       // mass
                30.0,      // wanderDistance
                30.0       // wanderRadius
        );

        this.setRadius(300.0);
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