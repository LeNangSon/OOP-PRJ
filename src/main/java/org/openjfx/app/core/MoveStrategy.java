package org.openjfx.app.core;

import java.util.List;

import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;

public interface MoveStrategy {
    
    void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt);
}