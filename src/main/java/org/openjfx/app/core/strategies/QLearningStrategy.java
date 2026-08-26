package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.Random;

import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.qlearning.QTable;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.entities.staticobjs.Plant;

public class QLearningStrategy implements MoveStrategy {

    public enum Role { PREDATOR, PREY }

    public static final int NUM_ACTIONS = 8;
    private static final double[] HUNT_LEADS = {0.0, HunterStrategy.DEFAULT_LEAD_TIME, 1.3, 2.0};

    private static final double THIRST_HIGH = 60.0;     
    private static final double THIRST_CRITICAL = 80.0; 
    private static final double THIRST_SATED = 25.0;    
    private static final double HUNGER_SEEK = 60.0;     
    private static final double HUNT_HUNGER = 50.0;    
    private static final double THIRST_PENALTY = 0.2;   
    private static final double HUNGER_PENALTY = 0.2;   
    private static final double THIRST_CRITICAL_PENALTY = 0.4; 
    private static final boolean COARSE_QL = Boolean.getBoolean("coarseQL");

    private static final boolean LEAN_REWARD = Boolean.getBoolean("lean");

    private final QTable q;
    private final Role role;
    private final double alpha;
    private final double gamma;
    private double epsilon;
    private final boolean training;
    private final Random rng;

    private MoveStrategy[] options;

    private String prevState;
    private int prevAction = -1;
    private double prevTargetDist = -1;
    private double prevEnemyDist = -1;
    private int prevTargetType = 0;

    private boolean pendingCaught;
    private boolean pendingAte;
    private boolean pendingDrank;

    private boolean thirstCommit;

    private double curTargetDist = -1;
    private double curEnemyDist = -1;
    private double curVision = 1.0;
    private int curTargetType = 0;
    private boolean curThirsty;
    private double curThirstLevel;           
    private double curHungerLevel;         

    public QLearningStrategy(QTable q, Role role, double alpha, double gamma,
                             double epsilon, boolean training, Random rng) {
        this.q = q;
        this.role = role;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.training = training;
        this.rng = rng != null ? rng : new Random();
    }

    public static QLearningStrategy play(QTable q, Role role) {
        return play(q, role, new Random());
    }


    public static QLearningStrategy play(QTable q, Role role, Random rng) {
        return new QLearningStrategy(q, role, 0.0, 0.97, 0.0, false, rng);
    }

    public void setEpsilon(double epsilon) { this.epsilon = epsilon; }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (!owner.isAlive()) return;

        String state = encode(owner, neighbors, world);   // đặt cur* fields

        // Học từ chuyển trạng thái của bước trước (giờ đã thấy hệ quả).
        if (training && prevState != null) {
            double reward = stepReward();
            q.update(prevState, prevAction, reward, state, alpha, gamma);
        }

        // Chọn macro-action rồi ỦY QUYỀN cho strategy luật (nó tự A* + ăn/uống/đẻ).
        int action = q.selectAction(state, epsilon, rng);
        double hungerBefore = owner.getHunger();
        double thirstBefore = owner.getThirst();
        options(owner)[action].updateVelocity(owner, neighbors, dt, world);

        // Phát hiện sự kiện qua delta (đặt cờ cho phần thưởng của bước kế tiếp).
        double hungerDrop = hungerBefore - owner.getHunger();
        double thirstDrop = thirstBefore - owner.getThirst();
        pendingCaught = role == Role.PREDATOR && hungerDrop > 1.0;   // Carnivore.eat đặt hunger=0 khi giết
        pendingAte    = role == Role.PREY && hungerDrop > 0.01;
        pendingDrank  = thirstDrop > 0.01;

        prevState = state;
        prevAction = action;
        prevTargetDist = curTargetDist;
        prevEnemyDist = curEnemyDist;
        prevTargetType = curTargetType;
    }

    // 8 strategy luật (khởi tạo lười, giữ state nội bộ qua các bước như RBS).
    private MoveStrategy[] options(LivingEntity owner) {
        if (options == null) {
            options = new MoveStrategy[] {
                    new FleeStrategy(),                                                          // 0
                    new SeekWaterStrategy(owner.getWanderDistance(), owner.getWanderRadius()),   // 1
                    new MateStrategy(),                                                          // 2
                    new WanderStrategy(owner.getWanderDistance(), owner.getWanderRadius()),      // 3
                    new HunterStrategy(HUNT_LEADS[0]),                                           // 4 direct
                    new HunterStrategy(HUNT_LEADS[1]),                                           // 5 short ~ RBS
                    new HunterStrategy(HUNT_LEADS[2]),                                           // 6 long
                    new HunterStrategy(HUNT_LEADS[3]),                                           // 7 ambush
            };
        }
        return options;
    }

    public void learnTerminal() {
        if (!training || prevState == null || prevAction < 0) return;
        q.update(prevState, prevAction, -10.0, null, alpha, gamma);
        prevState = null;
        prevAction = -1;
    }

    private double stepReward() {
        double r;
        if (role == Role.PREDATOR) {
            r = -0.02;                          // base reward
            if (pendingCaught) r += 10.0;       // bắt được mồi
        } else {
            r = 0.05;                           
            if (pendingAte) r += 1.0;           // ăn cỏ
        }
        if (curThirstLevel > THIRST_HIGH) {
            r -= THIRST_PENALTY * (curThirstLevel - THIRST_HIGH) / (100.0 - THIRST_HIGH);
        }
        if (!LEAN_REWARD && curThirstLevel > THIRST_CRITICAL) {
            r -= THIRST_CRITICAL_PENALTY * (curThirstLevel - THIRST_CRITICAL) / (100.0 - THIRST_CRITICAL);
        }
        if (!LEAN_REWARD && curHungerLevel > HUNT_HUNGER) {
            r -= HUNGER_PENALTY * (curHungerLevel - HUNT_HUNGER) / (100.0 - HUNT_HUNGER);
        }
        if (prevTargetType == curTargetType && prevTargetDist >= 0 && curTargetDist >= 0) {
            double closed = prevTargetDist - curTargetDist;
            if (closed > 0) r += 0.03 * closed;    
        }
        if (role == Role.PREY && prevEnemyDist >= 0 && curEnemyDist >= 0) {
            r += 0.06 * (curEnemyDist - prevEnemyDist);
        }
        return r;
    }

    // ---------------rời rạc hoá trạng thái
    private String encode(LivingEntity owner, List<Entity> neighbors, WorldMap world) {
        Vector2D pos = owner.getPosition();
        double vision = Math.max(owner.getVisionRadius(), 1.0);
        curVision = vision;

        Vector2D enemy = nearestScaredOf(owner, neighbors);
        curEnemyDist = enemy == null ? -1 : pos.distance(enemy);

        curThirstLevel = owner.getThirst();
        curHungerLevel = owner.getHunger();
        thirstCommit = thirstCommit
                ? owner.getThirst() > THIRST_SATED
                : owner.getThirst() >= THIRST_HIGH;
        curThirsty = thirstCommit;

        int targetType;
        Vector2D target;
        Entity preyEntity = null;          
        if (curThirsty) {
            target = world.findNearestTerrainPositionInRadius(pos, TerrainType.WATER, vision);
            targetType = target == null ? 0 : 2;
        } else {
            Entity prey = nearestPreyEntity(owner, neighbors, world);
            boolean wantPrey = role == Role.PREDATOR
                    ? owner.getHunger() >= HUNT_HUNGER
                    : owner.getHunger() >= HUNGER_SEEK;
            if (prey != null && wantPrey) {
                target = prey.getPosition();
                targetType = 1;
                preyEntity = prey;
            } else {
                target = null;
                targetType = 0;
            }
        }
        curTargetType = targetType;
        curTargetDist = target == null ? -1 : pos.distance(target);

        int enemyDir = enemy == null ? 8 : sector(enemy.sub(pos));
        int enemyBin = distBin(curEnemyDist, vision);
        int targetDir = target == null ? 8 : sector(target.sub(pos));
        int targetBin = distBin(curTargetDist, vision);
        int hungerBin, thirstBin;
        if (COARSE_QL) {
            hungerBin = curHungerLevel >= 50 ? 1 : 0;
            thirstBin = curThirsty ? 1 : 0;
        } else {
            hungerBin = curHungerLevel >= 90 ? 3 : curHungerLevel >= 70 ? 2 : curHungerLevel >= 50 ? 1 : 0;
            thirstBin = curThirstLevel >= THIRST_CRITICAL ? 2 : curThirstLevel >= THIRST_HIGH ? 1 : 0;
        }
        int preyMoveDir = preyMovementSector(preyEntity);
        int mateBin = (owner.canReproduce() && owner.hasMateNearby()) ? 1 : 0;

        return (role == Role.PREDATOR ? "P" : "R")
                + "|e" + enemyDir + ',' + enemyBin
                + "|t" + targetType + ',' + targetDir + ',' + targetBin
                + "|m" + preyMoveDir
                + "|h" + hungerBin
                + "|w" + thirstBin
                + "|p" + mateBin;
    }

    private int preyMovementSector(Entity prey) {
        if (prey instanceof LivingEntity le) {
            Vector2D v = le.getVelocity();
            if (v != null && v.magnitude() > 1.0) return sector(v);
        }
        return 8;
    }

    private int sector(Vector2D d) {
        double angle = Math.atan2(d.y, d.x);             
        int s = (int) Math.round(angle / (Math.PI / 4));
        return ((s % 8) + 8) % 8;                         
    }

    private int distBin(double dist, double vision) {
        if (dist < 0) return 0;                            // không có
        if (dist < vision / 3) return 1;                   // gần
        if (dist < 2 * vision / 3) return 2;               // vừa
        return 3;                                          // xa
    }

    private Entity nearestPreyEntity(LivingEntity owner, List<Entity> neighbors, WorldMap world) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity n : neighbors) {
            if (n == null || !RelationManager.isPrey(n.getType(), owner.getType())) continue;
            if (n instanceof Plant p && !p.isAlive()) continue;
            if (world.getTerrainAt(n.getPosition()) == TerrainType.BUSH) continue;  // mồi núp trong bụi
            double d = owner.getPosition().distance(n.getPosition());
            if (d < bestDist) { bestDist = d; best = n; }
        }
        return best;
    }

    private Vector2D nearestScaredOf(LivingEntity owner, List<Entity> neighbors) {
        Vector2D best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity n : neighbors) {
            if (n == null || !RelationManager.isScaredOf(owner.getType(), n.getType())) continue;
            double d = owner.getPosition().distance(n.getPosition());
            if (d < bestDist) { bestDist = d; best = n.getPosition(); }
        }
        return best;
    }
}
