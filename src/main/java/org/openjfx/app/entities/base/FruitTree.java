package org.openjfx.app.entities.base;
import org.openjfx.app.core.Vector2D;

public class FruitTree extends StaticEntity {
    private boolean hasFruit; // còn tồn tại hay đã bị ăn
    private double regrowTime; // thời gian mọc lại
    private double timer;

    public FruitTree(Vector2D position) {
        super(position, 10, "rectangle"); // size=10, shape=rectangle,
        this.hasFruit = true;
        this.regrowTime = 300;
        this.timer = 0;
    }
    
    public void harvestFruit() {
        if(hasFruit) {
            hasFruit = false;
            timer = 0;
        }
    }

    public boolean hasFruit() {
        return hasFruit;
    }

    @Override
    public void update(double dt) {
        if(!hasFruit) {
            timer += dt;
            if(timer >= regrowTime) {
                hasFruit = true;
                timer = 0;
            }
        }
    }
}
