package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public class WanderStrategy implements MoveStrategy {

    public static final class DebugWanderState {
        private final Vector2D circleCenter;
        private final double wanderRadius;
        private final Vector2D randomPoint;

        public DebugWanderState(Vector2D circleCenter, double wanderRadius, Vector2D randomPoint) {
            this.circleCenter = circleCenter;
            this.wanderRadius = wanderRadius;
            this.randomPoint = randomPoint;
        }

        public Vector2D getCircleCenter() {
            return circleCenter;
        }

        public double getWanderRadius() {
            return wanderRadius;
        }

        public Vector2D getRandomPoint() {
            return randomPoint;
        }
    }

    private static final Map<Integer, DebugWanderState> DEBUG_STATES = new ConcurrentHashMap<>();

    private double wanderDistance;
    private double wanderRadius;
    private double wanderTheta = Math.PI / 2;

    public WanderStrategy(double wanderDistance,double wanderRadius) {
        this.wanderDistance = wanderDistance;
        this.wanderRadius = wanderRadius;
    }

    public static DebugWanderState getDebugState(int entityId) {
        return DEBUG_STATES.get(entityId);
    }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        Vector2D currentVel = owner.getVelocity();
        Vector2D currentPos = owner.getPosition();

        double maxSpeed = owner.getMaxSpeed();
        double maxForce = owner.getMaxForce();
        double mass = owner.getMass();

        double safeWanderDistance = this.wanderDistance;
        double safeWanderRadius = this.wanderRadius;

        Vector2D forward = currentVel.magnitude() < 0.01
                ? new Vector2D(1, 0)
                : currentVel.normalize();

        Vector2D circleCenter = currentPos.add(forward.multiply(safeWanderDistance));

        double heading = Math.atan2(forward.y, forward.x);
        double theta = wanderTheta + heading;
        Vector2D offset = new Vector2D(Math.cos(theta), Math.sin(theta)).multiply(safeWanderRadius);
        Vector2D wanderPoint = circleCenter.add(offset);

        DEBUG_STATES.put(owner.getId(), new DebugWanderState(circleCenter, safeWanderRadius, wanderPoint));

        double displaceRange = 0.25;
        wanderTheta += (-displaceRange + Math.random() * 2 * displaceRange);

        Vector2D steer = wanderPoint.sub(currentPos);
        if (steer.magnitude() > 1e-6) {
            steer = steer.normalize().multiply(maxForce);
        } else {
            steer = new Vector2D(0, 0);
        }

        Vector2D acceleration = steer.multiply(1.0/mass);
        owner.setAcceleration(acceleration);

        Vector2D nextVelocity = currentVel.add(acceleration.multiply(dt));
        if (nextVelocity.magnitude() > maxSpeed && maxSpeed > 0) {
            nextVelocity = nextVelocity.normalize().multiply(maxSpeed);
        }
        owner.setVelocity(nextVelocity);
    }
}