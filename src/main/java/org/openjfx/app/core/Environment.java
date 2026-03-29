package org.openjfx.app.core;

import org.openjfx.app.entities.base.Entity;

public abstract class Environment extends Entity {
    protected double width;
    protected double height;

    public Environment(Vector2D position, double width, double height, String shape) {
        super(position, width, shape);
        this.width = width;
        this.height = height;
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    //Kiểm tra xem một thực thể có nằm trong vùng này không
    public boolean contains(Vector2D point) {
        return point.x >= position.x && point.x <= position.x + width &&
               point.y >= position.y && point.y <= position.y + height;
    }

    @Override
    public void update(double dt, WorldMap world) {

    }
}