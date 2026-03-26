package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;

public abstract class MovableEntity extends Entity {
    protected Vector2D velocity;

    public MovableEntity(Vector2D position, double size, String shape) {
        super(position, size, shape);
        this.velocity = new Vector2D(0, 0);
    }

    @Override
    public void update(double dt) {
        move(dt);
    }

    public void move(double dt) {
        this.position = this.position.add(this.velocity.multiply(dt));     // pos_new = pos_old + v * dt
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity;
    }
}