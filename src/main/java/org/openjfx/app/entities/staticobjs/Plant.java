package org.openjfx.app.entities.staticobjs;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.StaticEntity;

public abstract class Plant extends StaticEntity {
    private static final double DEFAULT_NUTRITION     = 30.0;
    private static final double SEEDLING_DURATION     = 5.0;   // giây ở giai đoạn mầm
    private static final double GROWING_DURATION      = 15.0;  // giây ở giai đoạn lớn dần
    private static final int    MAX_PLANTS_IN_RADIUS  = 6;     // giới hạn mật độ
    private static final double DENSITY_CHECK_RADIUS  = 80.0;  // bán kính kiểm tra mật độ

    public enum GrowthStage { SEEDLING, GROWING, MATURE, AGING }

    protected boolean alive;
    protected double nutrition;
    protected final double baseNutrition;
    protected final double baseSize;

    protected double reproduceTime;
    protected double reproduceTimer;

    private GrowthStage stage;
    private double stageTimer;
    private final double matureDuration; // 30–70 giây ở trạng thái trưởng thành
    private final double agingDuration;  // 10–30 giây ở trạng thái lão hóa

    public Plant(Vector2D position, double size, String shape, double reproduceTime) {
        this(position, size, shape, reproduceTime, DEFAULT_NUTRITION);
    }

    public Plant(Vector2D position, double size, String shape,
                 double reproduceTime, double nutrition) {
        super(position, size, shape);
        this.reproduceTime  = reproduceTime;
        this.reproduceTimer = 0;
        this.baseNutrition  = nutrition;
        this.baseSize       = size;
        this.alive          = true;

        this.stage          = GrowthStage.SEEDLING;
        this.stageTimer     = 0;
        this.matureDuration = 30 + Math.random() * 40;
        this.agingDuration  = 10 + Math.random() * 20;

        // Bắt đầu là mầm non: nhỏ và ít dinh dưỡng
        this.size      = size * 0.3;
        this.nutrition = baseNutrition * 0.3;
    }

    public boolean isAlive() { return alive; }

    public GrowthStage getGrowthStage() { return stage; }

    // Ăn cây: trả về lượng dinh dưỡng hiện tại (cây trưởng thành cho nhiều hơn mầm non)
    public double consume() {
        if (!alive) return 0;
        alive = false;
        return nutrition;
    }

    @Override
    public void update(double dt, WorldMap world) {
        if (!alive) return;

        updateGrowth(dt);
        if (!alive) return; // chết già trong updateGrowth

        // Chỉ cây trưởng thành và lão hóa mới sinh sản
        if (stage == GrowthStage.MATURE || stage == GrowthStage.AGING) {
            reproduceTimer += dt;
            if (reproduceTimer >= reproduceTime) {
                // Không phải lúc nào cũng sinh sản: 75% xác suất mỗi chu kỳ
                if (Math.random() < 0.75) {
                    reproduce(world);
                }
                reproduceTimer = 0;
            }
        }
    }

    private void updateGrowth(double dt) {
        stageTimer += dt;
        switch (stage) {
            case SEEDLING: {
                double t = Math.min(stageTimer / SEEDLING_DURATION, 1.0);
                this.size      = baseSize * lerp(0.3, 0.6, t);
                this.nutrition = baseNutrition * 0.3;
                if (stageTimer >= SEEDLING_DURATION) advance();
                break;
            }
            case GROWING: {
                double t = Math.min(stageTimer / GROWING_DURATION, 1.0);
                this.size      = baseSize * lerp(0.6, 1.0, t);
                this.nutrition = baseNutrition * lerp(0.3, 1.0, t);
                if (stageTimer >= GROWING_DURATION) advance();
                break;
            }
            case MATURE: {
                this.size      = baseSize;
                this.nutrition = baseNutrition;
                if (stageTimer >= matureDuration) advance();
                break;
            }
            case AGING: {
                // Cây già dần: teo nhỏ và mất dinh dưỡng
                double t = Math.min(stageTimer / agingDuration, 1.0);
                this.size      = baseSize * lerp(1.0, 0.5, t);
                this.nutrition = baseNutrition * lerp(1.0, 0.4, t);
                if (stageTimer >= agingDuration) {
                    alive = false; // chết già tự nhiên
                }
                break;
            }
        }
    }

    private void advance() {
        GrowthStage[] stages = GrowthStage.values();
        int next = stage.ordinal() + 1;
        if (next < stages.length) {
            stage = stages[next];
        }
        stageTimer = 0;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    protected void reproduce(WorldMap world) {
        // Kiểm tra mật độ: không sinh sản nếu khu vực đã quá đông
        if (countNearbyPlants(world) >= MAX_PLANTS_IN_RADIUS) return;

        // Phân tán hạt giống: 80% rơi gần (±40px), 20% bay xa theo gió (±80px)
        double range = Math.random() < 0.8 ? 40 : 80;

        for (int attempt = 0; attempt < 10; attempt++) {
            // Phân phối tam giác (~chuông): tổng 2 số ngẫu nhiên đều tạo peak ở giữa
            double dx = (Math.random() + Math.random() - 1.0) * range;
            double dy = (Math.random() + Math.random() - 1.0) * range;
            Vector2D candidate = new Vector2D(position.x + dx, position.y + dy);
            if (canReproduceAt(world, candidate)) {
                world.addEntity(createNewPlant(candidate));
                return;
            }
        }
    }

    private int countNearbyPlants(WorldMap world) {
        int count = 0;
        for (Entity e : world.getEntities()) {
            if (e != this && e instanceof Plant
                    && position.distance(e.getPosition()) <= DENSITY_CHECK_RADIUS) {
                count++;
            }
        }
        return count;
    }

    // Lớp con override để giới hạn terrain (ví dụ Algae chỉ mọc trên WATER)
    protected boolean canReproduceAt(WorldMap world, Vector2D position) {
        return true;
    }

    protected abstract Plant createNewPlant(Vector2D position);
}
