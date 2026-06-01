package org.openjfx.app.entities.base;

import java.util.List;

import org.openjfx.app.core.RelationManager;
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
        if (target instanceof LivingEntity prey && prey.isAlive()) {
            prey.setHealth(0);
            setHunger(0);
            setHealth(200);
            setVelocity(new Vector2D(0, 0));
        }
    }

    private List<StrategyCandidate> buildCandidates() {
        return List.of(
                new StrategyCandidate(FleeStrategy::new, (e, n) -> hasThreat(e, n) ? 100.0 : 0.0),
                new StrategyCandidate(() -> new SeekWaterStrategy(wanderDistance, wanderRadius),
                        (e, n) -> e.getThirst() > 70.0 || e.getMoveStrategy() instanceof SeekWaterStrategy ? e.getThirst() / 100.0 + 0.2 : 0.0),
                new StrategyCandidate(HunterStrategy::new,
                        (e, n) -> hasPreyNearby() || e.getHunger() > 60.0 ? Math.max(0.75, e.getHunger() / 100.0) : 0.0),
                new StrategyCandidate(MateStrategy::new, (e, n) -> canReproduce() && hasMateNearby() ? 0.45 : 0.0),
                new StrategyCandidate(() -> new WanderStrategy(wanderDistance, wanderRadius), (e, n) -> 0.3)
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

    private boolean hasPreyNearby() {
        if (neighbors == null) return false;
        for (Entity neighbor : neighbors) {
            if (neighbor != null && RelationManager.isPrey(neighbor.getType(), getType())) {
                return true;
            }
        }
        return false;
    }
}
