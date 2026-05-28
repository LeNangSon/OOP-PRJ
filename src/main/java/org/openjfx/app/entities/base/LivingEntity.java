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
    private static final double COOLDOWN = 0.5;
    private double blockedCooldown;


    public double getWanderRadius() {
        return wanderRadius;
    }

    public double getWanderDistance() {
        return wanderDistance;
    }

    protected double wanderRadius;
    protected double wanderDistance;

    private final double hungerRate;
    private final double thirstRate;
    private boolean isAlive;
    protected double visionRadius;
    private final double wanderSpeed;
    protected List<Entity> neighbors;

    protected double age = 0.0;
    protected double matureAge = 5.0;
    protected double reproduceCooldown = 0.0;
    protected double reproduceCooldownMax = 20.0;
    protected double reproduceHungerCost = 30.0;
    protected double reproduceMinHealth = 50.0;


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
        this.blockedCooldown = COOLDOWN;
    }

    //Setter
    public void setHealth(double health) {
        // Máu tuỳ theo con vật
        this.health = health;
        
        if (this.health <= 0 && this.isAlive == true) {
            System.out.println("Death");
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

        age += dt;
        if (reproduceCooldown > 0) {
            reproduceCooldown -= dt;
        }

        // --- ĐOẠN SỬA: Logic hiển thị Tên#ID khi tử vong ---
        if (hunger >= 100 || thirst >= 100) {
            setHealth(this.health - 5*dt);

            if (this.health <= 0) {
                String reason = (hunger >= 100) ? "vì quá đói" : "vì quá khát";
                String entityNameWithId = this.getClass().getSimpleName() + "#" + this.getId();
                world.broadcastDeath(entityNameWithId + " đã chết " + reason);
                return;
            }
        }

        // Đi ngược lại nếu không vào được
        Vector2D nextPosition = this.position.add(this.velocity.multiply(dt));
        if (world.canStandOn(this, nextPosition)) {
            this.position = nextPosition;
            
        } else {
            this.velocity = this.velocity.multiply(-0.5);
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

    public boolean canReproduce() {
        return isAlive
                && age >= matureAge
                && reproduceCooldown <= 0
                && getHunger() < 50.0
                && getThirst() < 50.0
                && getHealth() >= reproduceMinHealth;
    }

    public boolean hasMateNearby() {
        if (neighbors == null) {
            return false;
        }
        for (Entity n : neighbors) {
            if (n instanceof LivingEntity
                    && n.getClass() == this.getClass()
                    && ((LivingEntity) n).canReproduce()) {
                return true;
            }
        }
        return false;
    }

    protected abstract LivingEntity createOffspring(Vector2D spawnPos);

    public void spawnOffspring(WorldMap world, LivingEntity mate) {
        if (!canReproduce() || mate == null || !mate.canReproduce()) {
            return;
        }
        Vector2D spawnPos = pickSafeSpawnPos(world);
        if (spawnPos == null) {
            // Vẫn áp cooldown để tránh retry mỗi frame và gây freeze vĩnh cửu
            applyReproductionCost();
            mate.applyReproductionCost();
            return;
        }
        LivingEntity child = createOffspring(spawnPos);
        world.queueSpawn(child);
        applyReproductionCost();
        mate.applyReproductionCost();

        String childName = child.getClass().getSimpleName() + "#" + child.getId();
        String parentName = this.getClass().getSimpleName() + "#" + this.getId();
        world.notifyAction(parentName, "sinh ra", childName);
    }

    protected void applyReproductionCost() {
        setHunger(getHunger() + reproduceHungerCost);
        reproduceCooldown = reproduceCooldownMax;
    }

    private Vector2D pickSafeSpawnPos(WorldMap world) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r = size * (1.0 + Math.random());
            Vector2D candidate = position.add(
                    new Vector2D(Math.cos(angle), Math.sin(angle)).multiply(r));
            if (world.canStandOn(this, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public abstract void eat(Entity target, double dt);
    public void drink(double dt){
        setThirst(this.thirst - 20.0*dt);
    };
}