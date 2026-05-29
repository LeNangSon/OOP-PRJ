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
            setHunger(this.getHunger() - plant.consume());
        }
    }

    private List<StrategyCandidate> buildCandidates() {
        return List.of(
            new StrategyCandidate(
                FleeStrategy::new,
                (e, n) -> {
                    if (!hasThreat(e, n)) return 0.0;
                    double urgency = Math.max(e.getThirst(), e.getHunger()) / 100.0;
                    if (urgency < 0.8) return 100.0;
                    if (e.getHealth() < 60 && urgency >= 0.9) return 0.0;
                    return Math.max(0.0, 100.0 * (1.0 - (urgency - 0.8) / 0.2));
                }
            ),
            new StrategyCandidate(
                () -> new SeekWaterStrategy(wanderDistance, wanderRadius),
                (e, n) -> e.getThirst() / 50.0
            ),
            new StrategyCandidate(
                HunterStrategy::new,
                (e, n) -> e.getHunger() / 100.0
            ),
            new StrategyCandidate(
                MateStrategy::new,
                (e, n) -> (canReproduce() && hasMateNearby()) ? 0.45 : 0.0
            ),
            new StrategyCandidate(
                () -> new WanderStrategy(wanderDistance, wanderRadius),
                (e, n) -> 0.3
            )
        );
    }

    @Override
    public void update(double dt, WorldMap world) {
        this.neighbors = world.getNeighbors(this, this.visionRadius);
        if (candidates == null) candidates = buildCandidates();

        this.moveStrategy = StrategyCandidate.selectBest(candidates, this.moveStrategy, this, neighbors);

        if (this.moveStrategy != null) {
            this.moveStrategy.updateVelocity(this, neighbors, dt, world);
        }

        super.update(dt, world);
    }
}