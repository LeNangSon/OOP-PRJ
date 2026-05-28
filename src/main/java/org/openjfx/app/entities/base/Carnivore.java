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

public abstract class Carnivore extends LivingEntity {

    private List<StrategyCandidate> candidates;

    public Carnivore(Vector2D position, double size, String shape, double initialHealth, double hungerRate, double thirstRate,
                     double maxSpeed, double maxForce, double mass,
                     double wanderDistance, double wanderRadius) {
        super(position, size, shape, initialHealth, hungerRate, thirstRate,
                maxSpeed, maxForce, mass, wanderDistance, wanderRadius);
    }

    @Override
    public void eat(Entity target, double dt) {
        if (target instanceof LivingEntity prey) {
            if (prey.isAlive()) {
                prey.setHealth(0);
                this.setHunger(0);
                this.setHealth(200);
                this.setVelocity(new Vector2D(0, 0));
            }
        }
    }

    private List<StrategyCandidate> buildCandidates() {
        return List.of(
            new StrategyCandidate(
                FleeStrategy::new,
                (e, n) -> hasThreat(e, n) ? 100.0 : 0.0
            ),
            new StrategyCandidate(
                () -> new SeekWaterStrategy(wanderDistance, wanderRadius),
                (e, n) -> e.getThirst() / 100.0
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