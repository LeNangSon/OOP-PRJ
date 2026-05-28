package org.openjfx.app.core.strategies;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openjfx.app.core.Vector2D;
import org.openjfx.app.core.WorldMap;
import org.openjfx.app.core.terrain.TerrainGrid;

/**
 * Bộ nhớ né đường cho mỗi entity (theo entityId).
 *
 * - Memory ô cấm (Phương án 1): khi entity bị kẹt, các ô của >1/2 path cũ được
 *   ghi nhớ với TTL (mặc định 5s). Lần tìm đường tiếp theo (mọi strategy)
 *   sẽ tránh các ô này.
 *
 * - Đếm thất bại (Phương án 3): mỗi target có 1 fail counter. Khi vượt
 *   FAIL_LIMIT trong khoảng FAIL_RESET_NS, target được đánh dấu "bỏ" trong
 *   GIVEUP_TTL_NS, tránh oscillation.
 */
public final class PathAvoidance {

    // Cắt ô cấm theo nửa path đầu, kẹp trong khoảng [MIN, MAX].
    private static final int MIN_BLOCKED_WAYPOINTS = 3;
    private static final int MAX_BLOCKED_WAYPOINTS = 20;

    // Mỗi ô cấm sống 5s, sau đó tự rơi khỏi memory.
    private static final long AVOID_TTL_NS = 5_000_000_000L;

    // Mỗi entity giữ tối đa 80 ô cấm để tránh "bít" toàn bản đồ.
    private static final int MAX_KEYS_PER_ENTITY = 80;

    // Sau FAIL_LIMIT lần fail liên tiếp (cách nhau <= FAIL_RESET_NS) thì bỏ target.
    private static final int FAIL_LIMIT = 3;
    private static final long FAIL_RESET_NS = 2_000_000_000L;
    private static final long GIVEUP_TTL_NS = 500_000_000L;

    private static final class State {
        final Map<String, Long> avoidExpireAt = new HashMap<>();
        final Map<Integer, Integer> failCount = new HashMap<>();
        final Map<Integer, Long> failLastNanos = new HashMap<>();
        final Map<Integer, Long> giveUpUntil = new HashMap<>();
    }

    private static final Map<Integer, State> STATES = new ConcurrentHashMap<>();

    private PathAvoidance() {}

    private static State stateOf(int entityId) {
        return STATES.computeIfAbsent(entityId, k -> new State());
    }

    public static void clearEntity(int entityId) {
        STATES.remove(entityId);
    }

    /**
     * Cộng dồn các ô của nửa-path đầu vào memory rồi trả về tập ô cấm còn sống.
     * Gọi khi entity vừa bị kẹt (blockedLastStep) và có path cũ.
     */
    public static Set<String> recordAndCollect(int entityId,
                                               List<Vector2D> lastPath,
                                               WorldMap world) {
        long now = System.nanoTime();
        State st = stateOf(entityId);
        if (lastPath != null && !lastPath.isEmpty() && world != null) {
            int halfPlusOne = (lastPath.size() / 2) + 1;
            int limit = Math.min(lastPath.size(),
                        Math.min(MAX_BLOCKED_WAYPOINTS,
                        Math.max(MIN_BLOCKED_WAYPOINTS, halfPlusOne)));
            for (int i = 0; i < limit; i++) {
                Vector2D point = lastPath.get(i);
                if (point == null) continue;
                TerrainGrid.GridCoordinate grid = world.worldToGrid(point);
                if (grid != null) {
                    st.avoidExpireAt.put(grid.getRow() + ":" + grid.getCol(),
                            now + AVOID_TTL_NS);
                }
            }
            trimIfTooLarge(st);
        }
        return collectAlive(st, now);
    }

    /** Lấy tập ô cấm còn sống mà không thêm gì mới. */
    public static Set<String> getAvoidedKeys(int entityId) {
        State st = STATES.get(entityId);
        if (st == null) return null;
        return collectAlive(st, System.nanoTime());
    }

    private static Set<String> collectAlive(State st, long now) {
        Iterator<Map.Entry<String, Long>> it = st.avoidExpireAt.entrySet().iterator();
        Set<String> alive = new HashSet<>();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() <= now) {
                it.remove();
            } else {
                alive.add(e.getKey());
            }
        }
        return alive.isEmpty() ? null : alive;
    }

    private static void trimIfTooLarge(State st) {
        if (st.avoidExpireAt.size() <= MAX_KEYS_PER_ENTITY) return;
        List<Map.Entry<String, Long>> entries = new ArrayList<>(st.avoidExpireAt.entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));
        int toRemove = st.avoidExpireAt.size() - MAX_KEYS_PER_ENTITY;
        for (int i = 0; i < toRemove; i++) {
            st.avoidExpireAt.remove(entries.get(i).getKey());
        }
    }

    /**
     * Ghi nhận 1 lần thất bại với target. Nếu vượt FAIL_LIMIT trong cửa sổ
     * FAIL_RESET_NS, target sẽ vào danh sách "give up" trong GIVEUP_TTL_NS.
     */
    public static void noteFail(int entityId, int targetId) {
        long now = System.nanoTime();
        State st = stateOf(entityId);
        Long last = st.failLastNanos.get(targetId);
        int count;
        if (last != null && now - last <= FAIL_RESET_NS) {
            count = st.failCount.getOrDefault(targetId, 0) + 1;
        } else {
            count = 1;
        }
        st.failLastNanos.put(targetId, now);
        if (count >= FAIL_LIMIT) {
            st.giveUpUntil.put(targetId, now + GIVEUP_TTL_NS);
            st.failCount.remove(targetId);
            st.failLastNanos.remove(targetId);
        } else {
            st.failCount.put(targetId, count);
        }
    }

    /** Khi tới được target → reset counter và bỏ giveUp cho target đó. */
    public static void noteSuccess(int entityId, int targetId) {
        State st = STATES.get(entityId);
        if (st == null) return;
        st.failCount.remove(targetId);
        st.failLastNanos.remove(targetId);
        st.giveUpUntil.remove(targetId);
    }

    /** Có nên bỏ qua target này không (đang trong giveUp window)? */
    public static boolean isGivenUp(int entityId, int targetId) {
        State st = STATES.get(entityId);
        if (st == null) return false;
        Long until = st.giveUpUntil.get(targetId);
        if (until == null) return false;
        if (until <= System.nanoTime()) {
            st.giveUpUntil.remove(targetId);
            return false;
        }
        return true;
    }
}
