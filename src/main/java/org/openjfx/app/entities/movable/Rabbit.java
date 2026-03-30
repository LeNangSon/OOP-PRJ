package org.openjfx.app.entities.movable;

import org.openjfx.app.core.EntityType;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.WanderStrategy;
import org.openjfx.app.entities.base.Herbivore;

public class Rabbit extends Herbivore {

    public Rabbit(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate){
        super(position, 20.0, "circle", 100.0, 1.0, 1.5);
        this.setRadius(40.0);
        this.type = EntityType.RABBIT;

        // 1. CẤP TỐC ĐỘ (BẮT BUỘC)
        this.setMaxSpeed(60.0);

        // 2. LẮP NÃO (BẮT BUỘC) - Nạp thẳng thuật toán lang thang vào đầu con thỏ
        this.moveStrategy = new WanderStrategy(30.0, 50.0);
    }

    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);
    }
}