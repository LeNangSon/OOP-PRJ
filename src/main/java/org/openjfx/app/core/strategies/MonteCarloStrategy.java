package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.Random;

import org.openjfx.app.core.DeathCause;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.montecarlo.MonteCarloAgent;
import org.openjfx.app.core.qlearning.QTable;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.entities.staticobjs.Plant;

/**
 * Chiến lược di chuyển dùng Monte Carlo Control.
 *
 * <p>Cấu trúc song song với {@link QLearningStrategy} nhưng KHÔNG cập nhật QTable ở mỗi
 * bước — thay vào đó ghi trajectory vào {@link MonteCarloAgent} rồi cập nhật một lần
 * ở cuối episode (khi entity chết hoặc hết maxSteps).</p>
 *
 * <p>Các fallback và guard sinh thái (hysteresis nước, gate đói sói, panic flee thỏ,
 * autoInteract sinh sản) giữ nguyên như QLearningStrategy — xem RULES.md.</p>
 *
 * <p>Hàm cần sinh viên tự implement: {@link #encode} và {@link #stepReward}.</p>
 */
public class MonteCarloStrategy implements MoveStrategy {

    public enum Role { PREDATOR, PREY }

    public static final int NUM_ACTIONS = 8;           // 8 hướng la bàn (N, NE, E, …)
    private static final double[][] DIRS = buildDirs();

    // --- Instrumentation (cho harness so sánh): bước RL "thuần" (applyAction 8 hướng) vs
    //     bước phải mượn chiến lược luật (fallback dùng A*: SeekWater/Mate/Flee). Đếm tĩnh.
    private static long pureActionSteps = 0;
    private static long fallbackSteps = 0;
    public static void resetActionCounters() { pureActionSteps = 0; fallbackSteps = 0; }
    public static long getPureActionSteps() { return pureActionSteps; }
    public static long getFallbackSteps() { return fallbackSteps; }
    private static final double STEERING_GAIN = 4.0;

    // Ngưỡng sinh thái — phải khớp với QLearningStrategy và RBS (xem RULES.md).
    private static final double THIRST_HIGH  = 60.0;
    private static final double THIRST_SATED = 25.0;
    private static final double HUNGER_SEEK  = 60.0;   // thỏ: tìm cỏ khi đói > ngưỡng này
    private static final double HUNT_HUNGER  = 50.0;   // sói: săn khi đói > ngưỡng này

    private final QTable         q;
    private final Role           role;
    private final double         alpha;
    private final double         gamma;
    private       double         epsilon;
    private final boolean        training;
    private final Random         rng;
    private final MonteCarloAgent agent;

    // Trạng thái bước trước — cần để tính reward ở bước sau (1-step lag, giống QL).
    private String  prevState;
    private int     prevAction     = -1;
    private double  prevTargetDist = -1;
    private double  prevEnemyDist  = -1;
    private int     prevTargetType = 0;

    // Lazy-init fallback strategies.
    private MoveStrategy fallbackSeekWater;
    private MoveStrategy fallbackWander;
    private MoveStrategy fallbackMate;
    private MoveStrategy fallbackFlee;

    // Cờ sự kiện từ autoInteract() bước trước — dùng trong stepReward().
    private boolean pendingCaught;
    private boolean pendingAte;
    private boolean pendingDrank;

    // Side-effects của encode() — dùng trong stepReward() và fallback logic.
    private boolean thirstCommit;
    private double  curTargetDist = -1;
    private double  curEnemyDist  = -1;
    private double  curVision     = 1.0;
    private int     curTargetType = 0;
    private boolean curThirsty;

    public MonteCarloStrategy(QTable q, Role role, double alpha, double gamma,
                              double epsilon, boolean training, Random rng) {
        this.q        = q;
        this.role     = role;
        this.alpha    = alpha;
        this.gamma    = gamma;
        this.epsilon  = epsilon;
        this.training = training;
        this.rng      = rng != null ? rng : new Random();
        this.agent    = new MonteCarloAgent();
    }

    /** Factory: play-only (không học), epsilon nhỏ để tránh kẹt vòng lặp tất định. */
    public static MonteCarloStrategy play(QTable q, Role role) {
        return play(q, role, new Random());
    }

    /**
     * Như {@link #play(QTable, Role)} nhưng nhận RNG có seed cho 5% bước epsilon. Dùng ở
     * harness so sánh (BrainCompare) để cùng seed ra cùng kết quả (so theo cặp tái lập được).
     */
    public static MonteCarloStrategy play(QTable q, Role role, Random rng) {
        return new MonteCarloStrategy(q, role, 0.0, 0.97, 0.05, false, rng);
    }

    public void setEpsilon(double epsilon) { this.epsilon = epsilon; }

    // ================================================================= main loop

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (!owner.isAlive()) return;

        String state = encode(owner, neighbors, world);  // đặt cur* fields

        // Điền reward cho (prevState, prevAction) vào buffer — MC không update Q ngay.
        if (training && prevState != null) {
            agent.setLastReward(stepReward());
        }

        // Phản xạ panic thỏ: địch < nửa tầm nhìn -> FleeStrategy luật (không học).
        // Bảng Q 8 hướng kém với né cận chiến liên tục; guard này giữ đàn thỏ sống.
        if (role == Role.PREY && curEnemyDist >= 0 && curEnemyDist < curVision * 0.5) {
            fallbackSteps++;
            if (fallbackFlee == null) fallbackFlee = new FleeStrategy();
            fallbackFlee.updateVelocity(owner, neighbors, dt, world);
            resetPrev();
            return;
        }

        // "Mù" (không thấy mục tiêu lẫn kẻ thù) -> fallback luật, ngắt chuỗi MC.
        if (curTargetType == 0 && curEnemyDist < 0) {
            fallbackSteps++;
            fallback(owner, curThirsty).updateVelocity(owner, neighbors, dt, world);
            resetPrev();
            return;
        }

        pureActionSteps++;
        int action = q.selectAction(state, epsilon, rng);
        applyAction(owner, action, dt);

        // Ghi (state, action) vào buffer; reward điền ở bước sau.
        if (training) agent.record(state, action);

        pendingCaught = false;
        pendingAte    = false;
        pendingDrank  = false;
        autoInteract(owner, neighbors, world, dt);

        prevState      = state;
        prevAction     = action;
        prevTargetDist = curTargetDist;
        prevEnemyDist  = curEnemyDist;
        prevTargetType = curTargetType;
    }

    // ================================================================= kết thúc episode

    /**
     * Entity chết: điền reward terminal -10, gọi {@link MonteCarloAgent#finishEpisode}.
     * Gọi từ trainer khi phát hiện entity không còn alive.
     */
    public void learnTerminal() {
        if (!training) return;
        // Bước cuối trước khi chết — reward -10 đắt hơn reward bắt mồi (+10).
        if (prevState != null) agent.setLastReward(-10.0);
        agent.finishEpisode(q, gamma, alpha);
        resetPrev();
    }

    /**
     * Episode hết maxSteps mà entity vẫn sống: cập nhật Q dựa trên trajectory đã tích.
     * Reward bước cuối điền 0 (không có terminal event).
     */
    public void learnEpisodeEnd() {
        if (!training) return;
        if (prevState != null) agent.setLastReward(0.0);
        agent.finishEpisode(q, gamma, alpha);
        resetPrev();
    }

    // ================================================================= CORE — sinh viên implement

    /**
     * [CORE — tự implement]
     *
     * <p>Tính phần thưởng cho (prevState, prevAction) dựa trên kết quả quan sát được.
     * Gọi tham khảo {@link QLearningStrategy} để biết cấu trúc reward đã được kiểm chứng
     * với BalanceCheck. Có thể dùng trực tiếp hoặc viết lại theo ý.</p>
     *
     * <p>Các field có sẵn để dùng:</p>
     * <ul>
     *   <li>{@code role} — PREDATOR (sói) hay PREY (thỏ)</li>
     *   <li>{@code pendingCaught} — sói vừa bắt được mồi bước trước</li>
     *   <li>{@code pendingAte}    — thỏ vừa ăn cỏ</li>
     *   <li>{@code pendingDrank}  — vừa uống nước</li>
     *   <li>{@code prevTargetDist}, {@code curTargetDist}   — khoảng cách mục tiêu</li>
     *   <li>{@code prevEnemyDist}, {@code curEnemyDist}    — khoảng cách kẻ thù</li>
     *   <li>{@code prevTargetType}, {@code curTargetType}  — loại mục tiêu (0/1/2)</li>
     *   <li>{@code curThirsty}, {@code curVision}</li>
     * </ul>
     */
    private double stepReward() {
        double r;
        if (role == Role.PREDATOR) {
            r = -0.02;                          // sức ép đói/thời gian
            if (pendingCaught) r += 10.0;       // bắt được mồi: phần thưởng lớn
            if (pendingDrank) r += 0.3;         // uống nước cũng là sống còn (chết khát = -10)
        } else {
            r = 0.05;                           // còn sống thêm 1 bước
            if (pendingAte) r += 1.0;
            if (pendingDrank) r += 0.3;
        }
        // Tiến lại gần mục tiêu ưu tiên (mồi với sói; cỏ/nước với thỏ). CHỈ khi mục tiêu
        // hai bước cùng loại: đổi loại (mồi -> nước lúc khát) làm hiệu khoảng cách vô nghĩa.
        if (prevTargetType == curTargetType && prevTargetDist >= 0 && curTargetDist >= 0) {
            r += 0.05 * (prevTargetDist - curTargetDist);
        }
        // SÓI (lever 2): thưởng ÁP SÁT để ép pha đớp cuối + phạt khi để mồi thoát khỏi tầm.
        if (role == Role.PREDATOR) {
            double pounce = curVision / 3.0;            // bằng ô "gần" của distBin
            if (curTargetType == 1 && curTargetDist >= 0 && curTargetDist < pounce) {
                r += 0.4 * (1.0 - curTargetDist / pounce);   // càng sát mồi càng thưởng
            }
            // Đang bám mồi mà mồi biến khỏi tầm -> phạt nhẹ. KHÔNG phạt khi vừa bắt được,
            // cũng KHÔNG phạt oan lúc cơn khát vượt ngưỡng làm mục tiêu đổi sang nước.
            if (!pendingCaught && !curThirsty && prevTargetType == 1 && curTargetType == 0) {
                r -= 0.5;
            }
        }
        // Thỏ: thưởng khi tăng khoảng cách với sói.
        if (role == Role.PREY && prevEnemyDist >= 0 && curEnemyDist >= 0) {
            r += 0.06 * (curEnemyDist - prevEnemyDist);
        }
        return r;
    }

    /**
     * [CORE — sinh viên tự implement]
     *
     * <p>Rời rạc hoá trạng thái quan sát thành chuỗi key cho QTable.
     * Phải đặt các field side-effect trước khi return:</p>
     * <ul>
     *   <li>{@code curVision}     ← owner.getVisionRadius()</li>
     *   <li>{@code curEnemyDist}  ← khoảng cách đến kẻ thù gần nhất (−1 nếu không thấy)</li>
     *   <li>{@code curTargetDist} ← khoảng cách đến mục tiêu (−1 nếu không có)</li>
     *   <li>{@code curTargetType} ← 0=không có, 1=mồi/cỏ, 2=nước</li>
     *   <li>{@code curThirsty}    ← true nếu đang trong chế độ seek water (hysteresis)</li>
     *   <li>{@code thirstCommit}  ← cập nhật hysteresis hai mức THIRST_HIGH/THIRST_SATED</li>
     * </ul>
     *
     * <p>Tham khảo {@link QLearningStrategy#encode} để biết format key đã dùng.
     * Có thể dùng cùng format (tái dùng qtable đã train) hoặc thiết kế lại.</p>
     *
     * <p>Helper có sẵn: {@link #nearestScaredOf}, {@link #nearestPreyEntity},
     * {@link #preyMovementSector}, {@link #sector}, {@link #distBin}.</p>
     */
    @SuppressWarnings("unused")
    private String encode(LivingEntity owner, List<Entity> neighbors, WorldMap world) {
        Vector2D pos = owner.getPosition();
        double vision = Math.max(owner.getVisionRadius(), 1.0);
        curVision = vision;

        Vector2D enemy = nearestScaredOf(owner, neighbors);
        curEnemyDist = enemy == null ? -1 : pos.distance(enemy);

        thirstCommit = thirstCommit
                ? owner.getThirst() > THIRST_SATED
                : owner.getThirst() >= THIRST_HIGH;
        curThirsty = thirstCommit;

        int targetType;
        Vector2D target;
        Entity preyEntity = null;          // giữ lại để đọc vận tốc mồi (đón đầu)
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
        int hungerBin = owner.getHunger() >= 50 ? 1 : 0;
        int thirstBin = curThirsty ? 1 : 0;
        // Hướng con mồi đang chạy: cho sói "đoán" nơi mồi tới mà cắt góc. 8 = đứng yên/không có.
        int preyMoveDir = preyMovementSector(preyEntity);

        return (role == Role.PREDATOR ? "P" : "R")
                + "|e" + enemyDir + ',' + enemyBin
                + "|t" + targetType + ',' + targetDir + ',' + targetBin
                + "|m" + preyMoveDir
                + "|h" + hungerBin
                + "|w" + thirstBin;
    }

    // ================================================================= action

    private void applyAction(LivingEntity owner, int action, double dt) {
        Vector2D dir      = new Vector2D(DIRS[action][0], DIRS[action][1]);
        Vector2D desired  = dir.multiply(owner.getMaxSpeed());
        Vector2D steering = desired.sub(owner.getVelocity());
        Vector2D accel    = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        Vector2D v        = owner.getVelocity().add(accel.multiply(dt));
        if (v.magnitude() > 1e-6) v = v.normalize().multiply(owner.getMaxSpeed());
        owner.setAcceleration(accel);
        owner.setVelocity(v);
    }

    // ================================================================= auto interact

    private void autoInteract(LivingEntity owner, List<Entity> neighbors, WorldMap world, double dt) {
        Entity prey = nearestPreyEntity(owner, neighbors, world);
        if (prey != null
                && owner.getPosition().distance(prey.getPosition()) < world.getInteractionDistance(owner, prey)) {
            boolean wasAlive    = prey instanceof LivingEntity le && le.isAlive();
            double  hungerBefore = owner.getHunger();
            owner.eat(prey, dt);
            if (role == Role.PREDATOR) {
                if (wasAlive && prey instanceof LivingEntity le2 && !le2.isAlive()) {
                    world.recordDeath(prey.getType(), DeathCause.PREDATION);
                    pendingCaught = true;
                }
            } else if (owner.getHunger() < hungerBefore) {
                pendingAte = true;
            }
        }

        if (owner.getThirst() > 5.0) {
            double drinkRange = Math.max(10.0, owner.getSize() * 0.5);
            if (world.findNearestTerrainPositionInRadius(owner.getPosition(), TerrainType.WATER, drinkRange) != null) {
                owner.drink(dt);
                pendingDrank = true;
            }
        }

        // Sinh sản khi chạm — fixedStrategy chặn MateStrategy thông thường nên phải
        // xử lý tại đây; thiếu thì quần thể RL tuyệt chủng (không bao giờ đẻ).
        if (owner.canReproduce()) {
            for (Entity n : neighbors) {
                if (n instanceof LivingEntity mate
                        && n.getClass() == owner.getClass()
                        && mate.canReproduce()
                        && owner.getPosition().distance(n.getPosition())
                                < world.getInteractionDistance(owner, n) * 1.5) {
                    owner.spawnOffspring(world, mate);
                    break;
                }
            }
        }
    }

    // ================================================================= helpers

    private MoveStrategy fallback(LivingEntity owner, boolean thirsty) {
        if (thirsty) {
            if (fallbackSeekWater == null)
                fallbackSeekWater = new SeekWaterStrategy(owner.getWanderDistance(), owner.getWanderRadius());
            return fallbackSeekWater;
        }
        if (owner.canReproduce() && owner.hasMateNearby()) {
            if (fallbackMate == null) fallbackMate = new MateStrategy();
            return fallbackMate;
        }
        if (fallbackWander == null)
            fallbackWander = new WanderStrategy(owner.getWanderDistance(), owner.getWanderRadius());
        return fallbackWander;
    }

    private void resetPrev() {
        prevState      = null;
        prevAction     = -1;
        prevTargetDist = -1;
        prevEnemyDist  = -1;
        prevTargetType = 0;
        pendingCaught  = false;
        pendingAte     = false;
        pendingDrank   = false;
    }

    // ----------------------------------------------------------------- lookup helpers (dùng trong encode)

    /** Tìm entity đáng sợ gần nhất trong danh sách neighbors. */
    protected Vector2D nearestScaredOf(LivingEntity owner, List<Entity> neighbors) {
        Vector2D best     = null;
        double   bestDist = Double.MAX_VALUE;
        for (Entity n : neighbors) {
            if (n == null || !RelationManager.isScaredOf(owner.getType(), n.getType())) continue;
            double d = owner.getPosition().distance(n.getPosition());
            if (d < bestDist) { bestDist = d; best = n.getPosition(); }
        }
        return best;
    }

    /** Tìm entity con mồi gần nhất (không tính mồi núp bụi). */
    protected Entity nearestPreyEntity(LivingEntity owner, List<Entity> neighbors, WorldMap world) {
        Entity best     = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity n : neighbors) {
            if (n == null || !RelationManager.isPrey(n.getType(), owner.getType())) continue;
            if (n instanceof Plant p && !p.isAlive()) continue;
            if (world.getTerrainAt(n.getPosition()) == TerrainType.BUSH) continue;
            double d = owner.getPosition().distance(n.getPosition());
            if (d < bestDist) { bestDist = d; best = n; }
        }
        return best;
    }

    /** Hướng di chuyển của mồi (0-7); 8 nếu đứng yên hoặc không phải LivingEntity. */
    protected int preyMovementSector(Entity prey) {
        if (prey instanceof LivingEntity le) {
            Vector2D v = le.getVelocity();
            if (v != null && v.magnitude() > 1.0) return sector(v);
        }
        return 8;
    }

    protected int sector(Vector2D d) {
        double angle = Math.atan2(d.y, d.x);            // 
        int s = (int) Math.round(angle / (Math.PI / 4));
        return (s + 8) % 8;
    }
    private static double[][] buildDirs() {
        double[][] dirs = new double[NUM_ACTIONS][2];
        for (int i = 0; i < NUM_ACTIONS; i++) {
            double a = i * Math.PI / 4;
            dirs[i][0] = Math.cos(a);
            dirs[i][1] = Math.sin(a);
        }
        return dirs;
    }

    /** Bin khoảng cách: 0=không có, 1=gần (<v/3), 2=vừa (<2v/3), 3=xa. */
    protected int distBin(double dist, double vision) {
        if (dist < 0) return 0;
        if (dist < vision / 3)       return 1;
        if (dist < 2 * vision / 3)   return 2;
        return 3;
    }

    // ----------------------------------------------------------------- constants exposed
    public static double getThirstHigh()  { return THIRST_HIGH;  }
    public static double getThirstSated() { return THIRST_SATED; }
    public static double getHungerSeek()  { return HUNGER_SEEK;  }
    public static double getHuntHunger()  { return HUNT_HUNGER;  }

}
