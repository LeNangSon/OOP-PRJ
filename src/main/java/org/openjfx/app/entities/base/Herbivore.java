package org.openjfx.app.entities.base;

import java.util.List;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.HunterStrategy;
import org.openjfx.app.core.strategies.MateStrategy;
import org.openjfx.app.core.strategies.SeekWaterStrategy;
import org.openjfx.app.core.strategies.StrategyCandidate;
import org.openjfx.app.core.strategies.WanderStrategy;
import org.openjfx.app.entities.staticobjs.Plant;

public abstract class Herbivore extends LivingEntity {

    private List<StrategyCandidate> candidates;

    public Herbivore(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate,
                     double maxSpeed, double maxForce, double mass,
                     double wanderDistance, double wanderRadius) {
        super(position, size, shape, initialHealth, hungerRate, thirstRate, maxSpeed, maxForce, mass, wanderDistance, wanderRadius);
    }

    @Override
    public void eat(Entity target, double dt) {
        if (target instanceof Plant plant) {
            setHunger(getHunger() - plant.consume());
        }
    }

    private List<StrategyCandidate> buildCandidates() {
        return List.of(
                new StrategyCandidate(FleeStrategy::new,
                        (e, n) -> hasThreat(e, n) ? 100.0 : 0.0),
                new StrategyCandidate(() -> new SeekWaterStrategy(wanderDistance, wanderRadius),
                        (e, n) -> {
                            if (e.getThirst() < 5.0) return 0.0; // no bị kẹt tại nước khi đã đủ
                            if (e.getThirst() > 70.0 || e.getMoveStrategy() instanceof SeekWaterStrategy)
                                return e.getThirst() / 100.0 + 0.2;
                            return 0.0;
                        }),
                new StrategyCandidate(HunterStrategy::new,
                        (e, n) -> {
                            if (e.getHunger() < 5.0) return 0.0; // no bị kẹt tìm ăn khi no
                            if (e.getHunger() > 60.0 || e.getMoveStrategy() instanceof HunterStrategy)
                                return e.getHunger() / 100.0;
                            return 0.0;
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
        neighbors = world.getNeighbors(this, visionRadius);
        if (candidates == null) candidates = buildCandidates();
        moveStrategy = StrategyCandidate.selectBest(candidates, moveStrategy, this, neighbors);
        if (moveStrategy != null && !isAvoidingBlockedPath()) {
            moveStrategy.updateVelocity(this, neighbors, dt, world);
        }
        super.update(dt, world);
    }
}
