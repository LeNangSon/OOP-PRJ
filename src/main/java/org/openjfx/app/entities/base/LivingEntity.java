package org.openjfx.app.entities.base;

import java.util.List;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.MoveStrategy;
import org.openjfx.app.core.strategies.WanderStrategy;



public abstract class LivingEntity extends MovableEntity {
    //Atribute
    protected MoveStrategy moveStrategy;
    private double hunger;
    private double thirst;
    private double health;
    protected double wanderRadius;
    protected double wanderDistance;

    private double hungerRate;
    private double thirstRate;
    private boolean isAlive;
    protected double radius;
    protected  List<Entity> neighbors;

    //Constructor
    public LivingEntity(Vector2D position, double size, String shape, double initialHealth,double hungerRate, double thirstRate,
                        double maxSpeed, double maxForce, double mass,
                        double wanderDistance, double wanderRadius){
        super(position, size, shape, maxSpeed, maxForce, mass);
        this.health = initialHealth;
        this.hungerRate = hungerRate;
        this.thirstRate = thirstRate;
        this.hunger = 0.0;
        this.thirst = 0.0;
        this.isAlive = true;
        this.wanderRadius = wanderRadius;
        this.wanderDistance = wanderDistance;
        this.moveStrategy = new WanderStrategy(this.wanderDistance, this.wanderRadius);
    }


    //Getter 
    public double getHealth() { return health; }
    public double getHunger() { return hunger; }
    public double getThirst() { return thirst; }
    public boolean isAlive() { return isAlive; }
    public double getRadius() { return radius; }
    public double getThirstRate(){ return thirstRate; }
    public double getHungerRate(){ return hungerRate; }

    //Setter
    public void setHealth(double health) {
        // Máu [0:100]
        this.health = Math.max(0, Math.min(100, health));
        
        
        if (this.health <= 0 && this.isAlive == true) {
            System.out.println("Death");
            this.isAlive = false;
        }
    }

    public void setHunger(double hunger) {
        // Đói thuộc [0:100]
        this.hunger = Math.max(0, Math.min(100, hunger));
    }

    public void setThirst(double thirst) {
        // Thirst thuộc [0;100]
        this.thirst = Math.max(0, Math.min(100, thirst));
    }

    public void setRadius(double radius){
        this.radius = Math.max(0, radius);
    }

    //Method
    @Override
    public void update(double dt, WorldMap world) {
        if (!isAlive) {
            return;
        }
        setHunger(this.hunger + hungerRate * dt);
        setThirst(this.thirst + thirstRate * dt);
        //Đói + Khát quá thì bị mất máu
        if (hunger >= 100 || thirst >= 100) {
            setHealth(this.health - 5*dt);
        }

        // Move only when the next position is valid for this entity on the terrain grid.
        Vector2D nextPosition = this.position.add(this.velocity.multiply(dt));
        if (world.canStandOn(this, nextPosition)) {
            this.position = nextPosition;
        } else {
            this.velocity = this.velocity.multiply(-1);
        }

        handleOutOfMap(world);

    }

    protected void handleOutOfMap(WorldMap world) {
        double halfSize = this.size * 0.5;
        double minX = halfSize;
        double minY = halfSize;
        double maxX = Math.max(minX, world.getWidth() - halfSize);
        double maxY = Math.max(minY, world.getHeight() - halfSize);

        double clampedX = Math.max(minX, Math.min(maxX, this.position.x));
        double clampedY = Math.max(minY, Math.min(maxY, this.position.y));

        boolean hitX = clampedX != this.position.x;
        boolean hitY = clampedY != this.position.y;
        if (hitX || hitY) {
            this.position = new Vector2D(clampedX, clampedY);

            double vx = this.velocity.x;
            double vy = this.velocity.y;
            if (hitX) {
                vx = -vx;
            }
            if (hitY) {
                vy = -vy;
            }

            this.velocity = new Vector2D(vx, vy).multiply(0.9);
        }
    }

    public boolean hasThreat(Entity owner, List<Entity> neighbors) {
        for (Entity neighbor : neighbors){
            if (RelationManager.isScaredOf(owner.getType(), neighbor.getType())){
                return true;
            }
        }
        return false;
        
    }


    public abstract void eat(Entity target, double dt);
    public void drink(double dt){
        setThirst(this.thirst - 20.0*dt);
    };
}
