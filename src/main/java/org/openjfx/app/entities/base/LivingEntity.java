package org.openjfx.app.entities.base;

import java.util.List;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.strategies.FleeStrategy;
import org.openjfx.app.core.strategies.MateStrategy;
import org.openjfx.app.core.strategies.MoveStrategy;
import org.openjfx.app.core.strategies.SeekWaterStrategy;
import org.openjfx.app.core.strategies.WanderStrategy;



public abstract class LivingEntity extends MovableEntity {

    protected ActionState currentState = ActionState.WANDER;
    // --- THÊM MỚI: Trạng thái hành động ---
    public enum ActionState {
        FLEE, EAT, DRINK, MATE, WANDER
    }

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
        this.blockedCooldown = Cooldown;
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
    // --- BẮT ĐẦU PHẦN UTILITY AI ---
    protected void decideAction(WorldMap world) {
        double bestScore = 0;
        ActionState bestAction = ActionState.WANDER; 

        // 1. Chạy trốn
        double fleeScore = evaluateFleeScore();
        if (fleeScore > bestScore) { bestScore = fleeScore; bestAction = ActionState.FLEE; }

        // 2. Tìm thức ăn
        double eatScore = evaluateEatScore();
        if (eatScore > bestScore) { bestScore = eatScore; bestAction = ActionState.EAT; }

        // 3. Tìm nước uống
        double drinkScore = evaluateDrinkScore();
        if (drinkScore > bestScore) { bestScore = drinkScore; bestAction = ActionState.DRINK; }

        // 4. Sinh sản
        double mateScore = evaluateMateScore();
        if (mateScore > bestScore) { bestScore = mateScore; bestAction = ActionState.MATE; }

        // 5. Đi dạo
        double wanderScore = 30.0;
        if (wanderScore > bestScore) { bestScore = wanderScore; bestAction = ActionState.WANDER; }

        // Chuyển đổi trạng thái nếu có thay đổi
        if (this.currentState != bestAction) {
            this.currentState = bestAction;
            applyStrategyForState(bestAction);
        }
    }

    // Các hàm tính điểm mặc định (Subclass có thể ghi đè)
    protected double evaluateFleeScore() {
        return hasThreat(this, this.neighbors) ? 1000.0 : 0.0;
    }

    protected double evaluateEatScore() {
        return this.hunger; 
    }

    protected double evaluateDrinkScore() {
        // Thêm tính "bám dính" (hysteresis) để con vật uống xong mới làm việc khác
        if (this.currentState == ActionState.DRINK && this.thirst > 0.1) {
            return this.thirst + 50.0; // Ưu tiên uống cho xong
        }
        return this.thirst;
    }

    protected double evaluateMateScore() {
        return (canReproduce() && hasMateNearby()) ? 75.0 : 0.0;
    }

    // Áp dụng strategy tương ứng với state
    protected void applyStrategyForState(ActionState state) {
        switch (state) {
            case FLEE:
                this.setMoveStrategy(new FleeStrategy());
                break;
            case DRINK:
                this.setMoveStrategy(new SeekWaterStrategy(this.wanderDistance, this.wanderRadius));
                break;
            case MATE:
                this.setMoveStrategy(new MateStrategy());
                break;
            case WANDER:
                this.setMoveStrategy(new WanderStrategy(this.wanderDistance, this.wanderRadius));
                break;
            case EAT:
                // Sẽ được định nghĩa cụ thể trong Carnivore/Herbivore
                break;
        }
    }
    // --- KẾT THÚC PHẦN UTILITY AI ---
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

        // Cập nhật danh sách hàng xóm để các hàm đánh giá hành vi sử dụng
        this.neighbors = world.getNeighbors(this, this.visionRadius);

        // Quyết định hành động/chiến thuật dựa trên utility
        decideAction(world);

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
        if (neighbors == null) return false;
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