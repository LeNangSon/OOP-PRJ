package org.openjfx.app.entities.base;

import org.openjfx.app.core.FleeStrategy;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WanderStrategy;
import org.openjfx.app.core.WorldMap;




public abstract class Animal extends LivingEntity {

    public Animal(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate){
        super(position, size, shape, initialHealth, hungerRate, thirstRate);

    }


    
    @Override
    public void update(double dt, WorldMap world){
        super.update(dt, world);

        // Thứ tự ưu tiên : chạy trốn khỏi địch->sinh lý:ăn->đi lang thang (chưa bổ sung uống nước)
        if (hasThreat(this, neighbors)){
            this.moveStrategy = new FleeStrategy();
        }else if (this.getHunger() > 70.0 /*&& this.moveStrategy != HungerStrategy*/){
            /*this.moveStrategy = new HungerStrategy();*/   

        }else {
            this.moveStrategy = new WanderStrategy(this.wanderSpeed, this.wanderR);
        }

        


    }


    
}
