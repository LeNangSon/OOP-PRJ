package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class StrategyCandidate {
    private static final double HYSTERESIS = 0.10;

    private final Supplier<MoveStrategy> factory;
    private final BiFunction<LivingEntity, List<Entity>, Double> scorer;
    private MoveStrategy instance;

    public StrategyCandidate(Supplier<MoveStrategy> factory,
                             BiFunction<LivingEntity, List<Entity>, Double> scorer) {
        this.factory = factory;
        this.scorer = scorer;
    }

    public MoveStrategy getStrategy() {
        if (instance == null) instance = factory.get();
        return instance;
    }

    public static MoveStrategy selectBest(List<StrategyCandidate> candidates,
                                          MoveStrategy current,
                                          LivingEntity entity,
                                          List<Entity> neighbors) {
        StrategyCandidate winner = null;
        double winnerScore = Double.NEGATIVE_INFINITY;
        for (StrategyCandidate c : candidates) {
            double score = c.scorer.apply(entity, neighbors);
            if (c.instance == current) score += HYSTERESIS;
            if (score > winnerScore) {
                winnerScore = score;
                winner = c;
            }
        }
        return winner != null ? winner.getStrategy() : current;
    }
}
