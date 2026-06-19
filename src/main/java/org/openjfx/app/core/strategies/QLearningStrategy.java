package org.openjfx.app.core.strategies;

import java.util.List;
import java.util.Random;

import org.openjfx.app.core.DeathCause;
import org.openjfx.app.core.RelationManager;
import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.qlearning.QTable;
import org.openjfx.app.core.terrain.TerrainType;
import org.openjfx.app.entities.base.Entity;
import org.openjfx.app.entities.base.LivingEntity;
import org.openjfx.app.entities.staticobjs.Plant;

/**
 * Chiến lược di chuyển do Q-learning điều khiển. Dùng chung được cho sói (PREDATOR) và
 * thỏ (PREY): mỗi loài học một {@link QTable} riêng (chia sẻ giữa các cá thể cùng loài).
 *
 * <p>Vòng đời mỗi bước: quan sát -> rời rạc hoá thành trạng thái -> chọn 1 trong 8 hướng
 * (epsilon-greedy) -> đặt vận tốc. Việc ăn/uống khi chạm được xử lý tự động (cơ chế thế
 * giới, không phải hành động học). Phần thưởng được tính theo kiểu "trễ một bước": khi
 * bước kế tiếp tới, ta đã thấy hệ quả của hành động trước nên mới cập nhật Q.</p>
 *
 * <p>RL chỉ điều khiển khi CÓ thứ đáng quan tâm trong tầm nhìn (mục tiêu hoặc kẻ thù).
 * Khi "mù" (không thấy gì), trạng thái không chứa thông tin nào để học nên giao lại cho
 * luật: khát thì {@link SeekWaterStrategy} (biết đường tới nước ngoài tầm nhìn), không
 * thì {@link WanderStrategy}. Nhờ vậy con vật không bao giờ kẹt góc map vì một ô Q rỗng.</p>
 */
public class QLearningStrategy implements MoveStrategy {

    public enum Role { PREDATOR, PREY }

    public static final int NUM_ACTIONS = 8;            // 8 hướng la bàn
    private static final double[][] DIRS = buildDirs();

    // --- Instrumentation (cho harness so sánh): bước RL "thuần" (applyAction 8 hướng) vs
    //     bước phải mượn chiến lược luật (fallback dùng A*: SeekWater/Mate/Flee). Đếm tĩnh.
    private static long pureActionSteps = 0;
    private static long fallbackSteps = 0;
    public static void resetActionCounters() { pureActionSteps = 0; fallbackSteps = 0; }
    public static long getPureActionSteps() { return pureActionSteps; }
    public static long getFallbackSteps() { return fallbackSteps; }
    private static final double STEERING_GAIN = 4.0;

    private static final double THIRST_HIGH = 60.0;     // khát tới đây -> mục tiêu ưu tiên là nước
    // Hysteresis như RBS (THIRST_SATED): đã vào "chế độ uống" thì uống tới khi tụt dưới
    // mức này mới thôi. Không có nó, con vật RL uống tụt xuống 59.9 là bỏ đi săn ngay ->
    // sống cả đời ở mức khát 55-75, một pha rượt mồi dài là đủ chết khát.
    private static final double THIRST_SATED = 25.0;
    // Thỏ chỉ coi cỏ là mục tiêu khi đói >60 — PHẢI khớp ngưỡng của Herbivore RBS.
    // Bài học đắt: để 40 thì thỏ RL mải đuổi cỏ đúng trong "cửa sổ sinh sản" (canReproduce
    // cần đói <50) -> không bao giờ rảnh để ghép đôi -> đàn thỏ RL chết mòn về 0 dù chỉ
    // bị săn lác đác (đo 3 run 900s đều tuyệt chủng với tổng sinh ~0).
    private static final double HUNGER_SEEK = 60.0;
    // Sói RL cũng phải có ngưỡng "no thì kệ thỏ" NHƯ sói luật gốc (HUNT_HUNGER_START=50):
    // không gate thì sói RL săn 24/7 -> áp lực săn gấp mấy lần RBS -> đàn thỏ tuyệt chủng
    // (cân bằng game được chỉnh quanh nhịp săn-nghỉ của RBS). Trainer thả sói đói sẵn
    // (hunger>ngưỡng) để episode ngắn vẫn có pha rượt mà học.
    private static final double HUNT_HUNGER = 50.0;

    private final QTable q;
    private final Role role;
    private final double alpha;
    private final double gamma;
    private double epsilon;
    private final boolean training;
    private final Random rng;

    // Bộ nhớ chuyển trạng thái (s, a) của bước trước để cập nhật Q ở bước sau.
    private String prevState;
    private int prevAction = -1;
    private double prevTargetDist = -1;
    private double prevEnemyDist = -1;
    private int prevTargetType = 0;

    // Chiến lược luật dùng khi không thấy gì trong tầm nhìn (khởi tạo lười theo chủ thể).
    private MoveStrategy fallbackSeekWater;
    private MoveStrategy fallbackWander;
    private MoveStrategy fallbackMate;
    private MoveStrategy fallbackFlee;

    // Cờ sự kiện do autoInteract() của bước trước đặt, dùng cho phần thưởng bước sau.
    private boolean pendingCaught;
    private boolean pendingAte;
    private boolean pendingDrank;

    // true = đang trong "chế độ uống" (hysteresis 2 mức THIRST_HIGH/THIRST_SATED).
    private boolean thirstCommit;

    // Kết quả phụ của encode(), tái dùng cho hàm thưởng (khỏi tính khoảng cách 2 lần).
    private double curTargetDist = -1;
    private double curEnemyDist = -1;
    private double curVision = 1.0;          // tầm nhìn của bước hiện tại (cho thưởng áp sát)
    private int curTargetType = 0;           // 0=không có, 1=mồi/cỏ, 2=nước
    private boolean curThirsty;              // thirst >= THIRST_HIGH ở bước hiện tại

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

    /**
     * Tạo agent chỉ để chơi (khai thác bảng đã học, không cập nhật Q). Giữ epsilon nhỏ
     * thay vì 0: policy tham lam thuần tất định có thể kẹt vòng lặp giữa 2 trạng thái;
     * 5% bước ngẫu nhiên đủ phá kẹt mà không làm hỏng hành vi đã học.
     */
    public static QLearningStrategy play(QTable q, Role role) {
        return play(q, role, new Random());
    }

    /**
     * Như {@link #play(QTable, Role)} nhưng nhận RNG có seed cho 5% bước epsilon. Dùng ở
     * harness so sánh (BrainCompare) để chạy lại CÙNG seed ra CÙNG kết quả — nếu để
     * {@code new Random()} không seed thì nhánh RL nhiễu, phá thiết kế so theo cặp.
     */
    public static QLearningStrategy play(QTable q, Role role, Random rng) {
        return new QLearningStrategy(q, role, 0.0, 0.97, 0.05, false, rng);
    }

    public void setEpsilon(double epsilon) { this.epsilon = epsilon; }

    @Override
    public void updateVelocity(LivingEntity owner, List<Entity> neighbors, double dt, WorldMap world) {
        if (!owner.isAlive()) return;

        String state = encode(owner, neighbors, world);   // đặt curTargetDist, curEnemyDist

        // Học từ chuyển trạng thái của bước trước (giờ đã thấy hệ quả).
        if (training && prevState != null) {
            double reward = stepReward();
            q.update(prevState, prevAction, reward, state, alpha, gamma);
        }

        // Không thấy mục tiêu lẫn kẻ thù -> trạng thái không có thông tin để học,
        // giao cho luật: khát thì đi tìm nước (biết đường ngoài tầm nhìn), có bạn tình
        // thì lại gần (không có dòng này quần thể RL tuyệt chủng vì fixedStrategy chặn
        // MateStrategy chạy theo đường chấm điểm thông thường), không thì lang thang.
        // Ngắt chuỗi học để bước RL kế tiếp bắt đầu chuyển trạng thái mới.
        // PHẢN XẠ HOẢNG LOẠN (chỉ thỏ): địch áp sát trong nửa tầm nhìn -> giao cho luật
        // FleeStrategy chạy trối chết, như phản xạ điểm 100 của RBS. Bảng Q (8 hướng rời
        // rạc) né cận chiến kém hơn hình học liên tục -> không có dòng này thỏ RL là mồi
        // dễ, đàn thỏ tuyệt chủng khi bật ql. RL chỉ học vùng "thấy địch từ xa" (né sớm).
        if (role == Role.PREY && curEnemyDist >= 0 && curEnemyDist < curVision * 0.5) {
            fallbackSteps++;
            if (fallbackFlee == null) fallbackFlee = new FleeStrategy();
            fallbackFlee.updateVelocity(owner, neighbors, dt, world);
            prevState = null;
            prevAction = -1;
            prevTargetDist = -1;
            prevEnemyDist = -1;
            prevTargetType = 0;
            pendingCaught = false;
            pendingAte = false;
            pendingDrank = false;
            return;
        }

        if (curTargetType == 0 && curEnemyDist < 0) {
            fallbackSteps++;
            fallback(owner, curThirsty).updateVelocity(owner, neighbors, dt, world);
            prevState = null;
            prevAction = -1;
            prevTargetDist = -1;
            prevEnemyDist = -1;
            prevTargetType = 0;
            pendingCaught = false;
            pendingAte = false;
            pendingDrank = false;
            return;
        }

        pureActionSteps++;
        int action = q.selectAction(state, epsilon, rng);
        applyAction(owner, action, dt);

        // Tương tác khi chạm (ăn/uống) -> đặt cờ sự kiện cho phần thưởng của bước kế tiếp.
        pendingCaught = false;
        pendingAte = false;
        pendingDrank = false;
        autoInteract(owner, neighbors, world, dt);

        prevState = state;
        prevAction = action;
        prevTargetDist = curTargetDist;
        prevEnemyDist = curEnemyDist;
        prevTargetType = curTargetType;
    }

    /** Chiến lược luật dùng khi "mù": SeekWater lúc khát, MateStrategy khi có thể sinh
     *  sản và có bạn tình quanh đó, Wander lúc bình thường. */
    private MoveStrategy fallback(LivingEntity owner, boolean thirsty) {
        if (thirsty) {
            if (fallbackSeekWater == null) {
                fallbackSeekWater = new SeekWaterStrategy(owner.getWanderDistance(), owner.getWanderRadius());
            }
            return fallbackSeekWater;
        }
        if (owner.canReproduce() && owner.hasMateNearby()) {
            if (fallbackMate == null) {
                fallbackMate = new MateStrategy();
            }
            return fallbackMate;
        }
        if (fallbackWander == null) {
            fallbackWander = new WanderStrategy(owner.getWanderDistance(), owner.getWanderRadius());
        }
        return fallbackWander;
    }

    /** Gọi khi cá thể chết (trạng thái kết thúc): áp phần thưởng âm cho hành động cuối. */
    public void learnTerminal() {
        if (!training || prevState == null || prevAction < 0) return;
        // Chết phải đắt hơn hẳn một lần bắt mồi (+10), nếu không sói học "liều ăn nhiều".
        q.update(prevState, prevAction, -10.0, null, alpha, gamma);
        prevState = null;
        prevAction = -1;
    }

    // ----------------------------------------------------------------- phần thưởng
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

    // -------------------------------------------------------------- rời rạc hoá trạng thái
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

    /** Sector hướng di chuyển của mồi (0-7); 8 nếu mồi đứng yên hoặc không phải vật thể động. */
    private int preyMovementSector(Entity prey) {
        if (prey instanceof LivingEntity le) {
            Vector2D v = le.getVelocity();
            if (v != null && v.magnitude() > 1.0) return sector(v);
        }
        return 8;
    }

    private int sector(Vector2D d) {
        double angle = Math.atan2(d.y, d.x);              // -pi..pi
        int s = (int) Math.round(angle / (Math.PI / 4));  // -4..4
        return ((s % 8) + 8) % 8;                          // 0..7
    }

    private int distBin(double dist, double vision) {
        if (dist < 0) return 0;                            // không có
        if (dist < vision / 3) return 1;                   // gần
        if (dist < 2 * vision / 3) return 2;               // vừa
        return 3;                                          // xa
    }

    // ----------------------------------------------------------------- hành động
    private void applyAction(LivingEntity owner, int action, double dt) {
        Vector2D dir = new Vector2D(DIRS[action][0], DIRS[action][1]);
        Vector2D desired = dir.multiply(owner.getMaxSpeed());
        Vector2D steering = desired.sub(owner.getVelocity());
        Vector2D accel = steering.multiply(STEERING_GAIN).limit(owner.getMaxForce());
        Vector2D v = owner.getVelocity().add(accel.multiply(dt));
        if (v.magnitude() > 1e-6) v = v.normalize().multiply(owner.getMaxSpeed());
        owner.setAcceleration(accel);
        owner.setVelocity(v);
    }

    // ----------------------------------------------------- tương tác khi chạm (cơ chế thế giới)
    private void autoInteract(LivingEntity owner, List<Entity> neighbors, WorldMap world, double dt) {
        Entity prey = nearestPreyEntity(owner, neighbors, world);
        if (prey != null
                && owner.getPosition().distance(prey.getPosition()) < world.getInteractionDistance(owner, prey)) {
            boolean wasAlive = prey instanceof LivingEntity le && le.isAlive();
            double hungerBefore = owner.getHunger();
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

        // Sinh sản khi chạm bạn tình — cũng là cơ chế thế giới như ăn/uống. RL chiếm
        // moveStrategy nên nhánh MateStrategy thông thường không chạy; thiếu đoạn này
        // con vật RL không bao giờ đẻ -> quần thể tuyệt chủng dần (đo được khi bật ql).
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

    private static double[][] buildDirs() {
        double[][] dirs = new double[NUM_ACTIONS][2];
        for (int i = 0; i < NUM_ACTIONS; i++) {
            double a = i * Math.PI / 4;
            dirs[i][0] = Math.cos(a);
            dirs[i][1] = Math.sin(a);
        }
        return dirs;
    }
}
