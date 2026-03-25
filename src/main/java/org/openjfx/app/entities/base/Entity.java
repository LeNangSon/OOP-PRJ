package org.openjfx.app.entities.base;

import org.openjfx.app.core.Vector2D;

public abstract class Entity {
    protected Vector2D position;
    protected final int id;
    protected double size;
    protected String shape;
    private static int nextId = 0;

    public Entity(Vector2D position, double size, String shape) {
        this.position = position;
        this.size = size;
        this.shape = shape;
        this.id = nextId++;
    }

    public Vector2D getPosition() { return position; }
    public int getId() { return id; }
    public double getSize() { return size; }

    public abstract void update();

    // public abstract void render(GraphicsContext gc);

    @Override
    public String toString() {
        return String.format("%s{id=%d, pos=(%.1f, %.1f), size=%.1f}",
                this.getClass().getSimpleName(), id, position.x, position.y, size);
    }
}