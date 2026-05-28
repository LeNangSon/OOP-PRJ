package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.StaticEntity;

public abstract class Plant extends StaticEntity {
    private static final double DEFAULT_NUTRITION = 20.0;

    protected boolean alive;
    protected double nutrition;

    protected double reproduceTime;
    protected double reproduceTimer;

    public Plant(Vector2D position, double size, String shape, double reproduceTime) {
        this(position, size, shape, reproduceTime, DEFAULT_NUTRITION);
    }

    public Plant(Vector2D position, double size, String shape,
                 double reproduceTime, double nutrition) {
        super(position, size, shape);
        this.reproduceTime = reproduceTime;
        this.reproduceTimer = 0;
        this.nutrition = nutrition;
        this.alive = true;
    }

    public boolean isAlive() {
        return alive;
    }

    // Ăn 1 nhát: cây chết và biến mất khỏi map (WorldMap.update sẽ remove).
    public double consume() {
        if (!alive) {
            return 0;
        }
        alive = false;
        return nutrition;
    }

    @Override
    public void update(double dt, WorldMap world) {
        if (!alive) {
            return;
        }
        reproduceTimer += dt;
        if (reproduceTimer >= reproduceTime) {
            reproduce(world);
            reproduceTimer = 0;
        }
    }

    protected void reproduce(WorldMap world) {
        // Thử vài vị trí ngẫu nhiên để chọn được ô hợp lệ (theo canReproduceAt).
        for (int attempt = 0; attempt < 8; attempt++) {
            double newX = position.x + (Math.random() * 60 - 30);
            double newY = position.y + (Math.random() * 60 - 30);
            Vector2D candidate = new Vector2D(newX, newY);
            if (canReproduceAt(world, candidate)) {
                world.addEntity(createNewPlant(candidate));
                return;
            }
        }
    }

    // Mặc định: cây nào cũng có thể mọc bất kỳ đâu (kể cả ngoài địa hình mong muốn).
    // Lớp con override để giới hạn (ví dụ Algae chỉ mọc trên WATER).
    protected boolean canReproduceAt(WorldMap world, Vector2D position) {
        return true;
    }

    protected abstract Plant createNewPlant(Vector2D position);
}
