package org.openjfx.app.entities.base;

import java.util.List;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.staticobjs.Plant;
import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.core.strategies.MateStrategy;
import org.openjfx.app.core.strategies.SeekWaterStrategy;
import org.openjfx.app.core.strategies.StrategyCandidate;
import org.openjfx.app.core.strategies.WanderStrategy;

public abstract class Carnivore extends LivingEntity {

    // "Tầm vồ": mồi gần hơn mức này (closeness 0..1 so với tầm nhìn, 1 = ngay sát) thì
    // chộp luôn dù đang khát/bận việc khác — phản xạ cơ hội, không bỏ miếng ăn trước mặt.
    private static final double POUNCE_CLOSENESS = 0.5;
    // Điểm khi vồ: thắng mọi nhu cầu thường (SeekWater tối đa ~1.2) nhưng vẫn thua bỏ chạy (100).
    private static final double POUNCE_HUNT_SCORE = 2.0;

    private List<StrategyCandidate> candidates;
    // Lưu world của frame hiện tại để hàm chấm điểm lọc mồi giống HunterStrategy (địa hình bụi).
    private WorldMap currentWorld;

    public Carnivore(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate,
                     double maxSpeed, double maxForce, double mass,
                     double wanderDistance, double wanderRadius) {
        super(position, size, shape, initialHealth, hungerRate, thirstRate,
                maxSpeed, maxForce, mass, wanderDistance, wanderRadius);
    }

    @Override
    public void eat(Entity target, double dt) {
        if (target instanceof LivingEntity prey && prey.isAlive()) {
            prey.setHealth(0);
            setHunger(0);
            setHealth(200);
            setVelocity(new Vector2D(0, 0));
        }
    }

    private List<StrategyCandidate> buildCandidates() {
        return List.of(
                new StrategyCandidate(FleeStrategy::new,
                        (e, n) -> hasThreat(e, n) ? 100.0 : 0.0),
                new StrategyCandidate(() -> new SeekWaterStrategy(wanderDistance, wanderRadius),
                        (e, n) -> {
                            // Đang uống dở -> uống cho tới khi hết khát hẳn (chống yo-yo ở mép nước).
                            if (e.getMoveStrategy() instanceof SeekWaterStrategy)
                                return e.getThirst() > THIRST_SATED ? DRINK_COMMIT_SCORE : 0.0;
                            // Chưa uống -> chỉ đi tìm nước khi đã khát đáng kể.
                            return e.getThirst() > THIRST_SEEK_START ? e.getThirst() / 100.0 + 0.2 : 0.0;
                        }),
                new StrategyCandidate(HunterStrategy::new,
                        (e, n) -> {
                            double closeness = nearestPreyCloseness(); // 0..1 theo tầm nhìn, 0 nếu không thấy mồi
                            // Mồi trong tầm vồ -> chộp luôn, không bỏ miếng ăn trước mặt chỉ vì
                            // đang khát/bận việc khác (vẫn nhường phản xạ bỏ chạy điểm 100).
                            if (closeness >= POUNCE_CLOSENESS) return POUNCE_HUNT_SCORE;
                            if (closeness <= 0 && e.getHunger() <= 60.0) return 0.0;
                            // Mồi còn xa: chấm theo độ đói + độ sát, "đã trót đuổi thì làm nốt".
                            return Math.max(0.75, e.getHunger() / 100.0) + 0.5 * closeness;
                        }),
                new StrategyCandidate(MateStrategy::new,
                        (e, n) -> canReproduce() && hasMateNearby() ? 0.45 : 0.0),
                new StrategyCandidate(() -> new WanderStrategy(wanderDistance, wanderRadius),
                        (e, n) -> 0.3)
        );
    }

    @Override
    public void update(double dt, WorldMap world) {
        setDrinking(false);
        currentWorld = world;
        neighbors = world.getNeighbors(this, visionRadius);
        if (candidates == null) candidates = buildCandidates();
        moveStrategy = StrategyCandidate.selectBest(candidates, moveStrategy, dt, this, neighbors);
        if (moveStrategy != null && !isAvoidingBlockedPath()) {
            moveStrategy.updateVelocity(this, neighbors, dt, world);
        }
        super.update(dt, world);
    }

    // Mức độ "sát mồi" của con mồi gần nhất trong tầm nhìn: 1.0 = ngay sát, 0.0 = ở rìa
    // tầm nhìn hoặc không có mồi. Dùng để chấm điểm HunterStrategy theo tiến độ săn.
    private double nearestPreyCloseness() {
        if (neighbors == null) return 0.0;
        double vision = Math.max(getVisionRadius(), 1.0);
        double best = 0.0;
        for (Entity neighbor : neighbors) {
            if (neighbor == null || !RelationManager.isPrey(neighbor.getType(), getType())) continue;
            // Lọc giống HunterStrategy.findClosestPrey: bỏ xác chết và mồi núp trong bụi
            // (sói không vào được) -> tránh chấm điểm cao cho con mồi mà rốt cuộc sẽ không đuổi.
            if (neighbor instanceof Plant plant && !plant.isAlive()) continue;
            if (currentWorld != null
                    && currentWorld.getTerrainAt(neighbor.getPosition()) == TerrainType.BUSH) continue;
            double closeness = 1.0 - getPosition().distance(neighbor.getPosition()) / vision;
            if (closeness > best) best = closeness;
        }
        return best;
    }
}
