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
    private boolean blockedLastStep;
    private static final double Cooldown = 0.5;
    private double blockedCooldown;


    public double getWanderRadius() {
        return wanderRadius;
    }

    public double getWanderDistance() {
        return wanderDistance;
    }

    protected double wanderRadius;
    protected double wanderDistance;

    private double hungerRate;
    private double thirstRate;
    private boolean isAlive;
    protected double visionRadius;
    private final double wanderSpeed;
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
        this.wanderSpeed = 20;
        this.moveStrategy = new WanderStrategy(this.wanderDistance, this.wanderRadius);
    }


    //Getter 
    public double getWanderSpeed() {
        return wanderSpeed;
    }

    public boolean getBlockedLastStep() {
        return blockedLastStep;
    }

    public boolean isAvoidingBlockedPath() {
        return blockedCooldown > 0;
    }
    public double getHealth() { return health; }
    public double getHunger() { return hunger; }
    public double getThirst() { return thirst; }
    public boolean isAlive() { return isAlive; }
    public double getVisionRadius() { return visionRadius; }
    public double getThirstRate(){ return thirstRate; }
    public double getHungerRate(){ return hungerRate; }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }
    public void setBlockedLastStep(boolean blockedLastStep) {
        this.blockedLastStep = blockedLastStep;
    }

    public void setBlockedCooldown() {
        this.blockedCooldown = Cooldown;
    }

    //Setter
    public void setHealth(double health) {
        // Máu tuỳ theo con vật
        this.health = health;
        
        if (this.health <= 0 && this.isAlive) {
            this.isAlive = false;
        }
    }

    public void setMoveStrategy(MoveStrategy moveStrategy) {
        this.moveStrategy = moveStrategy;
    }

    public void setHunger(double hunger) {
        // Đói thuộc [0:100]
        this.hunger = Math.max(0, Math.min(100, hunger));
    }

    public void setThirst(double thirst) {
        // Thirst thuộc [0;100]
        this.thirst = Math.max(0, Math.min(100, thirst));
    }

    public void setVisionRadius(double visionRadius){
        this.visionRadius = Math.max(0, visionRadius);
    }

    //Method
    @Override
    public void update(double dt, WorldMap world) {


        if (!isAlive) {
            return;
        }
        setHunger(this.hunger + hungerRate * dt);
        setThirst(this.thirst + thirstRate * dt);


        // --- ĐOẠN SỬA: Logic hiển thị Tên#ID khi tử vong ---
        if (hunger >= 100 || thirst >= 100) {
            setHealth(this.health - 5*dt);
            
            if (this.health <= 0) {
                String reason = (hunger >= 100) ? "vì quá đói" : "vì quá khát";
                // Lấy tên Class (Wolf, Rabbit...) nối với dấu # và ID
                String entityNameWithId = this.getClass().getSimpleName() + "#" + this.getId();
                world.broadcastDeath(entityNameWithId + " đã chết " + reason);
            }
        }

        // Đi ngược lại nếu không vào được
        Vector2D nextPosition = this.position.add(this.velocity.multiply(dt));
        if (world.canStandOn(this, nextPosition)) {
            this.position = nextPosition;
            
        } else {
            avoidBlockedDirection(world, dt);
            this.setBlockedLastStep(true);
            this.setBlockedCooldown();
        }

        if (blockedCooldown > 0) {
            blockedCooldown -= dt;
        } else {
            blockedLastStep = false;
        }

        handleOutOfMap(world);

    }

    private void avoidBlockedDirection(WorldMap world, double dt) {
        Vector2D currentVelocity = this.velocity;
        Vector2D forward = currentVelocity.magnitude() < 0.01
                ? new Vector2D(Math.cos(this.id), Math.sin(this.id))
                : currentVelocity.normalize();

        Vector2D[] candidates = {
                new Vector2D(-forward.y, forward.x),
                new Vector2D(forward.y, -forward.x),
                forward.multiply(-1),
                new Vector2D(-forward.y, forward.x).add(forward.multiply(-0.35)),
                new Vector2D(forward.y, -forward.x).add(forward.multiply(-0.35)),
                new Vector2D(Math.cos(System.nanoTime() + this.id), Math.sin(System.nanoTime() + this.id))
        };

        double escapeSpeed = Math.max(this.getMaxSpeed() * 0.85, this.getWanderSpeed());
        double probeDistance = Math.max(this.size * 0.45, escapeSpeed * Math.max(dt, 0.08));

        for (Vector2D candidate : candidates) {
            if (candidate.magnitude() < 0.01) {
                continue;
            }

            Vector2D direction = candidate.normalize();
            Vector2D escapePosition = this.position.add(direction.multiply(probeDistance));
            if (world.canStandOn(this, escapePosition)) {
                this.velocity = direction.multiply(escapeSpeed);
                this.position = this.position.add(direction.multiply(Math.min(probeDistance, this.size * 0.12)));
                return;
            }
        }

        this.velocity = forward.multiply(-escapeSpeed * 0.5);
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
