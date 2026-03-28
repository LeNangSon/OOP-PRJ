package org.openjfx.app.core;

import java.util.List;

import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class WanderStrategy implements MoveStrategy {
    protected double wanderR;
    protected double wanderSpeed;

    private Vector2D targetPos = null;

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        Vector2D currentPos = owner.getPosition();

        double stepDistance = wanderSpeed * dt;

        if (targetPos == null || currentPos.distance(targetPos) < stepDistance) {
            double x_random = Math.random() - 0.5;
            double y_random = Math.random() - 0.5;

            Vector2D vectoRandom = new Vector2D(x_random, y_random).normalize();
            Vector2D offset = vectoRandom.multiply(wanderR);

            targetPos = currentPos.add(offset);
        }

        Vector2D direct = currentPos.directionTo(targetPos);
        Vector2D newVelocity = direct.multiply(wanderSpeed);
        owner.setVelocity(newVelocity);
    }
}