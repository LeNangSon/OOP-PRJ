package org.openjfx.app.core.strategies;

import java.util.List;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class WanderStrategy implements MoveStrategy {

    private double wanderSpeed;
    private double wanderR;
    private Vector2D targetPos = null;

    public WanderStrategy(double wanderSpeed, double wanderR) {
        this.wanderSpeed = wanderSpeed;
        this.wanderR = wanderR;
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {

        Vector2D currentPos = owner.getPosition();
        double stepDistance = this.wanderSpeed * dt;

        if (targetPos == null || currentPos.distance(targetPos) < stepDistance) {
            double x_random = Math.random() - 0.5;
            double y_random = Math.random() - 0.5;

            Vector2D vectoRandom = new Vector2D(x_random, y_random).normalize();
            Vector2D offset = vectoRandom.multiply(this.wanderR);

            targetPos = currentPos.add(offset);
        }

        Vector2D direct = currentPos.directionTo(targetPos);
        Vector2D newVelocity = direct.multiply(this.wanderSpeed);
        owner.setVelocity(newVelocity);
    }
}