package org.openjfx.app.entities.base;
import org.openjfx.app.core.Vector2D;

public abstract class Plant extends StaticEntity {
    protected boolean isAvailable; // còn tồn tại hay đã bị ăn
    protected int regrowTime; // thời gian mọc lại
    protected int timer;
    public Plant(Vector2D position, double size, String shape, int regrowTime) {
        super(position, size, shape);
        this.regrowTime = regrowTime;
        this.timer = 0;
        this.isAvailable = true;
    }

    public void consume() {
        if (isAvailable) {
            isAvailable = false;
            timer = 0;
        }
    }

    @Override
    public void update(double dt) {// viet lai update() cho Plant de dem thoi gian regrow
        if (!isAvailable) {
            timer += dt;
            if (timer >= regrowTime) {
                isAvailable = true;
            }
        }
        
    }

    public boolean isAvailable() {
        return isAvailable;
    }
}